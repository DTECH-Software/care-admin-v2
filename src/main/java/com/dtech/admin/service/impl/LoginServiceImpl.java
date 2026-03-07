/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 1:52 PM
 * <p>
 */

package com.dtech.admin.service.impl;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.LoginRequestDTO;
import com.dtech.admin.dto.response.*;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.entityToDto.SystemUserMapper;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.LoginService;
import com.dtech.admin.util.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    @Autowired
    private final WebUserRolePageTaskRepository webUserRolePageTaskRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final ModelMapper modelMapper;

    @Autowired
    private final WebUserRepository webUserRepository;

    @Autowired
    private final WebPasswordPolicyRepository webPasswordPolicyRepository;

    @Autowired
    private final JwtUtil jwtUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final WebTokenRepository webTokenRepository;

    @Autowired
    private final Gson gson;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> login(LoginRequestDTO loginRequestDTO, Locale locale) {
        try {
            log.info("Login request: " + loginRequestDTO);

            log.info("Processing login request username:-{} password:-{} ", loginRequestDTO.getUsername(), loginRequestDTO.getPassword());
            String username = loginRequestDTO.getUsername().trim();
            String password = loginRequestDTO.getPassword().trim();

            Optional<WebUser> optionalUser = webUserRepository.
                    findByUsernameAndStatusIn(username, List.of(Status.ACTIVE, Status.INACTIVE));

            String masked = StringUtil.maskString(loginRequestDTO.getPassword());
            log.info("Masked: " + masked);
            return optionalUser.map(user -> {

                String hashPasswordRequest = "";
                try {
                    hashPasswordRequest = PasswordUtil.passwordEncoder(user.getUserKey(), password);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                log.info("password decoder:-{}", hashPasswordRequest);
                if (user.getPassword().equals(hashPasswordRequest)) {

                    if (user.isReset() || user.getLoginStatus() == Status.INACTIVE) {
                        log.info("user is reset state or inactive {}", user.getUsername());
                        loginRequestDTO.setPassword(masked);
                        auditLogService.log(com.dtech.admin.enums.WebPage.LOGIN.name(), WebTask.LOGIN.name(), loginRequestDTO.getIp(), AuditTask.USER_INACTIVE_OR_FORCE_TO_CHANGE_PASSWORD.getDescription(), loginRequestDTO.getUserAgent(), gson.toJson(loginRequestDTO.toString()), null, loginRequestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1004, messageSource.getMessage(ResponseMessageUtil.LOGIN_STATUS_INACTIVE_OR_EXPECTED_RESET, null, locale)));
                    }

                    Optional<Integer> passwordPolicyAttemptCount = getPasswordPolicyAttemptCount();
                    if (user.getPasswordExpiredDate().before(DateTimeUtil.getCurrentDateTime())) {
                        log.info("User password {} is expired", username);
                        updatePasswordExpireLogin(user);
                        loginRequestDTO.setPassword(masked);
                        auditLogService.log(com.dtech.admin.enums.WebPage.LOGIN.name(), WebTask.LOGIN.name(), loginRequestDTO.getIp(), AuditTask.USER_LOGIN_TO_EXPIRED_PASSWORD_PERIOD_FORCE_TO_CHANGE_PASSWORD.getDescription(), loginRequestDTO.getUserAgent(), gson.toJson(loginRequestDTO.toString()), null, loginRequestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1004, messageSource.getMessage(ResponseMessageUtil.PASSWORD_EXPIRED_AT_LOGIN_TIME, null, locale)));
                    } else if (passwordPolicyAttemptCount.get() > 0 && user.getAttemptCount() > passwordPolicyAttemptCount.get()) {
                        log.info("User password {} is attempt exceed", user.getAttemptCount());
                        updatePasswordExpireLogin(user);
                        loginRequestDTO.setPassword(masked);
                        auditLogService.log(com.dtech.admin.enums.WebPage.LOGIN.name(), WebTask.LOGIN.name(), loginRequestDTO.getIp(), AuditTask.USER_ATTEMPT_LIMIT_EXCEED.getDescription(), loginRequestDTO.getUserAgent(), gson.toJson(loginRequestDTO.toString()), null, loginRequestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1004, messageSource.getMessage(ResponseMessageUtil.PASSWORD_ATTEMPT_EXCEED, null, locale)));
                    }

                    String accessToken = jwtUtil.generateAccessToken(username);
                    log.info("Generate JWT token access {} ", accessToken);
                    String refreshToken = jwtUtil.generateRefreshToken(username);
                    log.info("Generate JWT token refresh {} ", refreshToken);
                    List<GrantedAuthority> authorities = user.getAuthorities().stream()
                            .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                            .collect(Collectors.toList());
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword(), authorities);
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.info("Added security context to user {}", user.getUsername());
                    updateSuccessLogin(user, loginRequestDTO, accessToken, refreshToken);
                    TokenResponseDTO tokenResponseDTO = TokenResponseDTO.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken).build();
                    log.info("After successful update application user");
                    UserDetailsResponseDTO userDetailsResponseDTO = SystemUserMapper.mapUserDetailsForLogin(user);
                    Map<String, Object> res = new HashMap<>();
                    res.put("profileDetails", userDetailsResponseDTO);
                    res.put("tokenDetails", tokenResponseDTO);
                    loginRequestDTO.setPassword(masked);
                    auditLogService.log(null, WebTask.LOGIN.name(), loginRequestDTO.getIp(), AuditTask.USER_LOGIN_SUCCESS.getDescription(), loginRequestDTO.getUserAgent(), gson.toJson(loginRequestDTO.toString()), null, loginRequestDTO.getUsername());
                    return ResponseEntity.ok().body(responseUtil.success((Object) res, messageSource.getMessage(ResponseMessageUtil.AUTHENTICATION_SUCCESS, null, locale)));
                } else {
                    log.info("Processing login request password mismatch for username {} ", loginRequestDTO.getUsername());
                    updateWrongLogin(user);
                    loginRequestDTO.setPassword(masked);
                    auditLogService.log(com.dtech.admin.enums.WebPage.LOGIN.name(), WebTask.LOGIN.name(), loginRequestDTO.getIp(), AuditTask.USER_ENTERED_PASSWORD_MISMATCH.getDescription(), loginRequestDTO.getUserAgent(), gson.toJson(loginRequestDTO.toString()), null, loginRequestDTO.getUsername());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseUtil.error(null, 1005, messageSource.getMessage(ResponseMessageUtil.USER_NAME_OR_PASSWORD_INVALID, null, locale)));
                }
            }).orElseGet(() -> {
                log.info("Processing login request user not found for username {} ", loginRequestDTO.getUsername());
                loginRequestDTO.setPassword(masked);
                auditLogService.log(com.dtech.admin.enums.WebPage.LOGIN.name(), WebTask.LOGIN.name(), loginRequestDTO.getIp(), AuditTask.SYSTEM_USER_NOT_FOUND.getDescription(), loginRequestDTO.getUserAgent(), gson.toJson(loginRequestDTO.toString()), null, loginRequestDTO.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseUtil.error(null, 1006, messageSource.getMessage(ResponseMessageUtil.USER_NAME_OR_PASSWORD_INVALID, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    protected Optional<Integer> getPasswordPolicyAttemptCount() {
        try {
            log.info("Processing password policy attemptCount ");
            return webPasswordPolicyRepository.findPasswordPolicy().map(WebPasswordPolicy::getAttemptExceedCount);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateSuccessLogin(WebUser webUser, LoginRequestDTO loginRequestDTO, String accessToken, String refreshToken) {
        try {
            log.info("Processing success login request username {}", loginRequestDTO.getUsername());

            WebToken webToken = new WebToken();
            webToken.setAccessToken(accessToken);
            webToken.setRefreshToken(refreshToken);
            WebToken savedToken = webTokenRepository.saveAndFlush(webToken);
            log.info("Set new login request token {} ", loginRequestDTO.getUsername());
            webUser.setLoginStatus(Status.ACTIVE);
            webUser.setLastLoggedDate(DateTimeUtil.getCurrentDateTime());
            webUser.setPasswordExpiredDate(DateTimeUtil.get30FutureDate());
            webUser.setAttemptCount(0);
            webUser.setExpectingFirstTimeLogging(false);
            webUser.setWebToken(savedToken);
            webUserRepository.saveAndFlush(webUser);
            log.info("After success login request update data username {}", loginRequestDTO.getUsername());
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateWrongLogin(WebUser webUser) {
        try {
            log.info("Processing wrong login request  username {} attempt {} ",
                    webUser.getUsername(), webUser.getAttemptCount());
            Optional<Integer> attemptCount = getPasswordPolicyAttemptCount();
            if (attemptCount.get() > 0 && webUser.getAttemptCount() > attemptCount.get()) {
                log.info("Processing wrong login request username {} attempt {} login status {} ", webUser.getUsername()
                        , webUser.getAttemptCount(), webUser.getLoginStatus());
                webUser.setLoginStatus(Status.INACTIVE);
                webUser.setReset(true);
            }
            webUser.setAttemptCount(webUser.getAttemptCount() + 1);
            webUserRepository.saveAndFlush(webUser);
            log.info("After wrong login request update data username {}", webUser.getUsername());
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updatePasswordExpireLogin(WebUser webUser) {
        try {
            log.info("Processing update password login request  username {} attempt {} ",
                    webUser.getUsername(), webUser.getAttemptCount());
            webUser.setLoginStatus(Status.INACTIVE);
            webUser.setReset(true);
            webUserRepository.saveAndFlush(webUser);
            log.info("After update password login request update data username {}", webUser.getUsername());
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> leftMenu(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Get authorizations details {}", channelRequestDTO.getUsername());
            return webUserRepository.findByUsername(channelRequestDTO.getUsername().trim()).map((user) -> {
                        if (user.getUserRole().getStatus().equals(Status.ACTIVE)) {
                            List<WebUserRolePageTask> webUserRolePageTasks = webUserRolePageTaskRepository.findAllByRole(user.getUserRole());
                            Map<String, SectionPagesResponseDTO> sectionPageMap = new LinkedHashMap<>();

                            webUserRolePageTasks.stream()
                                    .filter(task -> {
                                        WebPage page = task.getPage();
                                        WebSection section = page.getWebSection();
                                        return page.getStatus() == Status.ACTIVE && section.getStatus() == Status.ACTIVE;
                                    })
                                    .forEach(task -> {
                                        WebSection section = task.getPage().getWebSection();
                                        String sectionCode = section.getCode();

                                        sectionPageMap.computeIfAbsent(sectionCode, code -> {
                                            SectionPagesResponseDTO dto = new SectionPagesResponseDTO();
                                            dto.setCode(section.getCode());
                                            dto.setDescription(section.getDescription());
                                            dto.setPages(new ArrayList<>());
                                            return dto;
                                        });

                                        List<WebPageResponseDTO> pages = sectionPageMap.get(sectionCode).getPages();
                                        boolean pageExists = pages.stream()
                                                .anyMatch(page -> page.getCode().equals(task.getPage().getCode()));

                                        if (!pageExists) {
                                            pages.add(modelMapper.map(task.getPage(), WebPageResponseDTO.class));
                                        }
                                    });

                            sectionPageMap.values().forEach(dto ->
                                    dto.getPages().sort(Comparator.comparing(WebPageResponseDTO::getDescription))
                            );

                            return ResponseEntity.ok().body(responseUtil.success((Object) sectionPageMap, messageSource.getMessage(ResponseMessageUtil.USER_AUTHORIZER_PAGES_RETRIEVED_SUCCESS, null, locale)));
                        } else {
                            log.info("User role inactive {} {} ", user.getUserRole().getId(), user.getUserRole().getCode());
                            return ResponseEntity.ok().body(responseUtil.error(null, 1002, messageSource.getMessage(ResponseMessageUtil.SYSTEM_USER_ROLE_INACTIVE, null, locale)));
                        }
                    })
                    .orElseGet(() -> {
                        log.info("User {} not found or login status inactive", channelRequestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1001, messageSource.getMessage(ResponseMessageUtil.SYSTEM_USER_NOT_FOUND_OR_INACTIVE, null, locale)));
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> logout(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Logout user {}", channelRequestDTO);
            return webUserRepository.findByUsername(channelRequestDTO.getUsername().trim()).map((user) -> {
                        user.setWebToken(null);
                        webUserRepository.saveAndFlush(user);
                        SecurityContextHolder.clearContext();
                        log.info("Logged out user success {}", channelRequestDTO);
                        return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.LOGOUT_SUCCESS, null, locale)));
                    })
                    .orElseGet(() -> {
                        log.info("User {} not found or logout", channelRequestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1001, messageSource.getMessage(ResponseMessageUtil.SYSTEM_USER_NOT_FOUND_OR_INACTIVE, null, locale)));
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}

/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 1:52 PM
 * <p>
 */

package com.dtech.admin.service.impl;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ResetPasswordDTO;
import com.dtech.admin.dto.response.*;
import com.dtech.admin.enums.*;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.WebUserAuditMapper;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.MessageService;
import com.dtech.admin.service.ResetPasswordService;
import com.dtech.admin.util.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements ResetPasswordService {

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private WebUserRepository webUserRepository;

    @Autowired
    private final WebPasswordPolicyRepository webPasswordPolicyRepository;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final WebPasswordHistoryRepository webPasswordHistoryRepository;

    @Autowired
    private final Gson gson;

    @Autowired
    private final MessageService messageService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> resetPassword(ResetPasswordDTO resetPasswordDTO, Locale locale) {
        log.info("processing reset password request {}", resetPasswordDTO);
        String username = resetPasswordDTO.getUsername().trim();
        String password = resetPasswordDTO.getConfirmPassword().trim();

       return webUserRepository.findByUsername(username).map(user -> {
            String hashPassword = "";
            try {
                log.info("processing reset password hash {}", password);
                hashPassword = PasswordUtil.passwordEncoder(user.getUserKey(), password);
            } catch (NoSuchAlgorithmException e) {
                log.error(e);
                throw new RuntimeException(e);
            }
            String message = validAlignCurrentPasswordPolicy(password, user, hashPassword);
            log.info("After reset password validation process {}", message);
            if (message == null || message.trim().isEmpty()) {
                user.setLoginStatus(Status.ACTIVE);
                user.setStatus(Status.ACTIVE);
                user.setPasswordExpiredDate(DateTimeUtil.get30FutureDate());
                user.setAttemptCount(0);
                user.setPassword(hashPassword);
                user.setLastPasswordChangeDate(DateTimeUtil.getCurrentDateTime());
                user.setReset(false);
                webUserRepository.saveAndFlush(user);
                log.info("password reset successfully completed {} {}", password, username);
                String masked = StringUtil.maskString(resetPasswordDTO.getConfirmPassword());
                resetPasswordDTO.setPassword(masked);
                auditLogService.log(WebPage.PASR.name(), WebTask.PASSWORD_RESET.name(),resetPasswordDTO.getIp(),AuditTask.PASSWORD_RESET_SUCCESS.getDescription(),resetPasswordDTO.getUserAgent(), gson.toJson(resetPasswordDTO.toString()),null,resetPasswordDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.PASSWORD_RESET_SUCCESS, null, locale)));
            }
            return ResponseEntity.ok().body(responseUtil.error(null, 1033, message));
        }).orElseGet(() -> {
           log.info("User {} not found", resetPasswordDTO.getUsername());
           return ResponseEntity.ok().body(responseUtil.error(null, 1001, messageSource.getMessage(ResponseMessageUtil.SYSTEM_USER_NOT_FOUND_OR_INACTIVE, null, locale)));
       });
    }

    @Transactional(readOnly = true)
    protected String validAlignCurrentPasswordPolicy(String password, WebUser webUser, String hashPassword) {
        try {
            log.info("Reset password align with current password policy {}", password);
            return webPasswordPolicyRepository.findPasswordPolicy()
                    .map(policy -> {
                        int charCount = StringUtil.getCharCount(password);
                        // max length check
                        if (charCount > policy.getMaxLength()) {
                            log.info("Reset password invalid max length validation {}", password);
                            return messageSource.getMessage("val.max.length.invalid", new Object[]{policy.getMaxLength()}, null);
                        }

                        // min length check
                        if (charCount < policy.getMinLength()) {
                            log.info("Reset password invalid min length validation char count {} policy min length {}", charCount, policy.getMinLength());
                            return messageSource.getMessage("val.min.length.invalid", new Object[]{policy.getMinLength()}, null);
                        }

                        int upperCount = StringUtil.countCharsByConditions(password, Character::isUpperCase);
                        // upper count check
                        if (upperCount < policy.getMinUpperCase()) {
                            log.info("Reset password invalid upper case validation {}", password);
                            return messageSource.getMessage("val.upper.length.invalid", new Object[]{policy.getMinUpperCase()}, null);
                        }

                        int lowerCount = StringUtil.countCharsByConditions(password, Character::isLowerCase);
                        // lower count check
                        if (lowerCount < policy.getMinLowerCase()) {
                            log.info("Reset password invalid lower case validation {}", password);
                            return messageSource.getMessage("val.lower.length.invalid", new Object[]{policy.getMinLowerCase()}, null);
                        }

                        int digitCount = StringUtil.countCharsByConditions(password, Character::isDigit);
                        // number count check
                        if (digitCount < policy.getMinNumbers()) {
                            log.info("Reset password invalid min digit validation {}", password);
                            return messageSource.getMessage("val.number.length.invalid", new Object[]{policy.getMinNumbers()}, null);
                        }

                        int specialCharCount = StringUtil.countCharsByConditions(password, c -> !Character.isLetterOrDigit(c));
                        // special char count check
                        if (specialCharCount < policy.getMinSpecialCharacters()) {
                            log.info("Reset password invalid special char validation {}", password);
                            return messageSource.getMessage("val.special.length.invalid", new Object[]{policy.getMinSpecialCharacters()}, null);
                        }

                        //check password history in used
                        if (policy.getPasswordHistory() > 0) {
                            Sort sort = Sort.by(Sort.Order.desc("createdDate"));
                            List<WebPasswordHistory> histories = webPasswordHistoryRepository
                                    .findByWebUserAndPassword(webUser,hashPassword,sort);

                            LocalDateTime localDateTime = LocalDateTime.now().minusDays(policy.getPasswordHistory());
                            boolean history = histories.stream().anyMatch((pw) -> pw.getCreatedDate().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime().isAfter(localDateTime));

                            if (history) {
                                log.info("Reset password invalid history validation {}", password);
                                return messageSource.getMessage("val.password.used.history", null, null);
                            }
                        }
                        log.info("Reset password success validation {}", password);
                        return "";
                    })
                    .orElseGet(() -> {
                        log.info("Password request policy not found for username {} ", password);
                        return messageSource.getMessage("val.password.policy.notfound", null, null);
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> requestOTP(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Request OTP request {}", channelRequestDTO);
            return webUserRepository.findByUsername(channelRequestDTO.getUsername()).map(user -> {
                log.info("Calling OTP generate sent password reset");
                String otp = RandomGeneratorUtil.getRandom6DigitNumber();
                log.info("OTP generated  sent password reset {}", otp);

                String hashPassword = "";

                try {
                    hashPassword = PasswordUtil.passwordEncoder(user.getUserKey(), otp);
                } catch (NoSuchAlgorithmException e) {
                    log.error("Password encoding failed for sent password reset", e);
                    throw new RuntimeException(e);
                }

                log.info("User mapping success: {}", user.getUsername());
                user.setPassword(hashPassword);
                user.setPasswordExpiredDate(DateTimeUtil.get1HourFutureDate());
                user.setReset(true);

                user = webUserRepository.saveAndFlush(user);
                updatePasswordHistoryByUser(user, hashPassword);

                List<String> newAuditList = WebUserAuditMapper.mapToDTOAudit(
                        List.of(user));

                auditLogService.log(
                        WebPage.URAM.name(),
                        WebTask.SENT_OTP_PASSWORD.name(),
                        AuditTask.PASSWORD_RESET_OTP_SENT_SUCCESS.getDescription(),
                        channelRequestDTO.getIp(),
                        channelRequestDTO.getUserAgent(),
                        gson.toJson(newAuditList),
                        null,
                        channelRequestDTO.getUsername()
                );
                log.info("Otp sent successfully {}", otp);
                messageService.sendMessage(MessageType.SENT_OTP_PASSWORD, otp, null,user.getMobile());
                log.info("Sent OTP, waiting for response...");

                return ResponseEntity.ok().body(
                        responseUtil.success(
                                null,
                                messageSource.getMessage(
                                        ResponseMessageUtil.PASSWORD_RESET_OTP_SENT_SUCCESS,
                                        null,
                                        locale
                                )
                        )
                );

            }).orElseGet(() -> {
                log.info("User send user not found {}", channelRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.SYSTEM_USER_NOT_FOUND_OR_INACTIVE, null, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updatePasswordHistoryByUser(WebUser webUser, String hashPassword) {
        try {
            log.info("Updating password history for reset password {}", webUser);
            WebPasswordHistory passwordHistory = new WebPasswordHistory();
            passwordHistory.setWebUser(webUser);
            passwordHistory.setPassword(hashPassword);
            webPasswordHistoryRepository.saveAndFlush(passwordHistory);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}

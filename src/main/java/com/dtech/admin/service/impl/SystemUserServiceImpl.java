/**
 * User: Himal_J
 * Date: 4/30/2025
 * Time: 10:35 AM
 * <p>
 */

package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.api.MessageResponseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.SystemUserRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.UserCommonResponseDTO;
import com.dtech.admin.dto.response.UsernamePolicyResponseDTO;
import com.dtech.admin.dto.search.SystemUserSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.WebUserAuditMapper;
import com.dtech.admin.mapper.dtoToEntity.SystemUserMapper;
import com.dtech.admin.mapper.entityToDto.UsernamePolicyMapper;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.MessageService;
import com.dtech.admin.service.SystemUserService;
import com.dtech.admin.specifications.UserSpecification;
import com.dtech.admin.util.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class SystemUserServiceImpl implements SystemUserService {

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final WebUserRepository webUserRepository;

    @Autowired
    private final WebUsernamePolicyRepository webUsernamePolicyRepository;

    @Autowired
    private final WebUserRoleRepository webUserRoleRepository;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final MessageService messageService;

    @Autowired
    private final WebPasswordHistoryRepository webPasswordHistoryRepository;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("User creation reference date {} ", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.URAM.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Status.values())
                    .filter(status -> !Status.DELETE.name().equals(status.name())).map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> approvalLevel = Arrays.stream(ApprovalLevel.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> deathLevel = Arrays.stream(ApprovalLevel.values()).filter(d -> !d.equals(ApprovalLevel.LEVEL03))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> userRole = webUserRoleRepository.findAllByWebUserRole(Status.ACTIVE);
            List<SimpleBaseDTO> companies = companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                    .map(company -> new SimpleBaseDTO(company.getCode(), company.getDescription()))
                    .toList();

            UsernamePolicyResponseDTO usernamePolicy = webUsernamePolicyRepository.findUsernamePolicy()
                    .map(UsernamePolicyMapper::mapUsernamePolicyDetails).orElse(null);

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            responseMap.put("userRole", userRole);
            responseMap.put("company", companies);
            responseMap.put("usernamePolicy", usernamePolicy);
            responseMap.put("approvalLevel", approvalLevel);
            responseMap.put("deathApprovalLevel", deathLevel);
            auditLogService.log(WebPage.URAM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.URAM.name()}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<SystemUserSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("User filter list page {} ", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            Page<WebUser> webUsers = Objects.nonNull(paginationRequest.getSearch()) ?
                    webUserRepository.findAll(UserSpecification.getSpecification(paginationRequest.getSearch(), paginationRequest.getUsername()), pageable) :
                    webUserRepository.findAll(UserSpecification.getSpecification(paginationRequest.getUsername()), pageable);
            log.info("User filter records {}", webUsers);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    webUserRepository.count(UserSpecification.getSpecification(paginationRequest.getSearch(), paginationRequest.getUsername())) :
                    webUserRepository.count(UserSpecification.getSpecification(paginationRequest.getUsername()));
            log.info("User filter records map start");
            List<UserCommonResponseDTO> responseDTOList = webUsers.stream()
                    .map(com.dtech.admin.mapper.entityToDto.SystemUserMapper::mapUserDetails).toList();
            log.info("User filter records map finish");
            List<String> newAuditList = WebUserAuditMapper.mapToDTOAudit(webUsers.stream().toList());
            auditLogService.log(WebPage.URAM.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<UserCommonResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.USER_FILTER_LIST_SUCCESS,
                            null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> add(SystemUserRequestDTO systemUserRequestDTO, Locale locale) {
        try {
            log.info("Add system user {}", systemUserRequestDTO);

            String username = systemUserRequestDTO.getNewUsername().trim();
            boolean exists = webUserRepository.existsByUsernameEqualsIgnoreCaseAndStatusNot(
                    username,Status.DELETE);

            if (exists) {
                log.info("User {} already exists", username);
                return ResponseEntity.ok().body(
                        responseUtil.error(
                                null,
                                1016,
                                messageSource.getMessage(
                                        ResponseMessageUtil.USER_NAME_ALREADY_EXIST,
                                        new Object[]{username},
                                        locale
                                )
                        )
                );
            }

            String isValid = validAlignCurrentUsernamePolicy(username);
            log.info("After user name validation {}", isValid);

            if (isValid == null || isValid.trim().isEmpty()) {
                return webUserRoleRepository.findByCodeAndStatus(systemUserRequestDTO.getUserRole(), Status.ACTIVE)
                        .map(userRole -> {
                            LinkedHashSet<CompanyTypes> companies = resolveRequestedCompanies(systemUserRequestDTO.getCompanies());
                            if (companies == null) {
                                return ResponseEntity.ok().body(
                                        responseUtil.error(
                                                null,
                                                1046,
                                                messageSource.getMessage(ResponseMessageUtil.COMPANY_NOT_FOUND, null, locale)
                                        )
                                );
                            }
                            log.info("Calling OTP generate");
                            String otp = RandomGeneratorUtil.getRandom6DigitNumber();
                            log.info("OTP generated {}", otp);

                            MessageResponseDTO messageResponseDTO =
                                    messageService.sendMessage(
                                            MessageType.USER_CREATION,
                                            otp,
                                            "Username: " + username,
                                            systemUserRequestDTO.getMobile()
                                    );
                            log.info("Sent OTP, waiting for response...");

//                            try {
//                                message = messageResponseDTO.get();
//                            } catch (Exception e) {
//                                log.error("Error while sending message", e);
//                                return ResponseEntity.ok().body(
//                                        responseUtil.error(
//                                                null,
//                                                1019,
//                                                messageSource.getMessage("message.sending.failed", null, locale)
//                                        )
//                                );
//                            }

                            if (messageResponseDTO.isSuccess()) {
                                log.info("Message sent successfully {}", messageResponseDTO);

                                String saltKey;
                                String hashPassword;
                                try {
                                    log.info("Generating SALT key");
                                    saltKey = PasswordUtil.generateSaltKey(
                                            systemUserRequestDTO.getNic().trim() +
                                                    DateTimeUtil.getCurrentDateTime()
                                    );
                                    log.info("Generated SALT key: {}", saltKey);
                                } catch (NoSuchAlgorithmException e) {
                                    log.error("Salt generation failed", e);
                                    throw new RuntimeException(e);
                                }

                                try {
                                    hashPassword = PasswordUtil.passwordEncoder(saltKey, otp);
                                } catch (NoSuchAlgorithmException e) {
                                    log.error("Password encoding failed", e);
                                    throw new RuntimeException(e);
                                }

                                WebUser webUser = SystemUserMapper.mapUserForAdd(systemUserRequestDTO);
                                log.info("User mapping success: {}", webUser.getUsername());
                                webUser.setUserKey(saltKey);
                                webUser.setPassword(hashPassword);
                                webUser.setUserRole(userRole);
                                webUser.setCompanies(companies);

                                webUser = webUserRepository.saveAndFlush(webUser);
                                updatePasswordHistoryByUser(webUser, hashPassword);

                                List<String> newAuditList = WebUserAuditMapper.mapToDTOAudit(
                                        List.of(webUser));

                                auditLogService.log(
                                        WebPage.URAM.name(),
                                        WebTask.ADD.name(),
                                        AuditTask.ADD_DATA.getDescription(),
                                        systemUserRequestDTO.getIp(),
                                        systemUserRequestDTO.getUserAgent(),
                                        gson.toJson(newAuditList),
                                        null,
                                        systemUserRequestDTO.getUsername()
                                );

                                return ResponseEntity.ok().body(
                                        responseUtil.success(
                                                null,
                                                messageSource.getMessage(
                                                        ResponseMessageUtil.USER_ADDED_SUCCESS,
                                                        new Object[]{systemUserRequestDTO.getNewUsername()},
                                                        locale
                                                )
                                        )
                                );
                            } else {
                                log.info("Message failed to send {}", messageResponseDTO);
                                return ResponseEntity.ok().body(
                                        responseUtil.error(
                                                null,
                                                1018,
                                                messageResponseDTO.getMessage()
                                        )
                                );
                            }
                        })
                        .orElseGet(() -> {
                            log.info("User role {} not found", systemUserRequestDTO.getUserRole());
                            return ResponseEntity.ok().body(
                                    responseUtil.error(
                                            null,
                                            1017,
                                            messageSource.getMessage(
                                                    ResponseMessageUtil.WEB_USER_ROLE_NOT_FOUND_OR_INACTIVE,
                                                    null,
                                                    locale
                                            )
                                    )
                            );
                        });
            }

            log.info("Username policy invalid for {}: {}", username, isValid);
            return ResponseEntity.ok().body(
                    responseUtil.error(null, 1017, isValid)
            );
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(SystemUserRequestDTO systemUserRequestDTO, Locale locale) {
        try {
            log.info("User view {}", systemUserRequestDTO);

            return webUserRepository.findById(systemUserRequestDTO.getId()).map(user -> {
                UserCommonResponseDTO userDetails = com.dtech.admin.mapper.entityToDto.SystemUserMapper.mapUserDetails(user);
                List<String> newAuditList = WebUserAuditMapper.mapToDTOAudit(List.of(user));
                auditLogService.log(WebPage.URAM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), systemUserRequestDTO.getIp(), systemUserRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, systemUserRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) userDetails, messageSource.getMessage(ResponseMessageUtil.USER_FOUND_BY_ID, new Object[]{systemUserRequestDTO.getId()}, locale)));
            }).orElseGet(() -> {
                log.info("User view not found {}", systemUserRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.USER_NOT_FOUND_BY_ID, new Object[]{systemUserRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(SystemUserRequestDTO systemUserRequestDTO, Locale locale) {
        try {
            log.info("User update {}", systemUserRequestDTO);
            return webUserRepository.findById(systemUserRequestDTO.getId()).map(user -> {

                String newModel = new StringBuilder()
                        .append(systemUserRequestDTO.getNic())
                        .append("|")
                        .append(systemUserRequestDTO.getEmail())
                        .append("|")
                        .append(systemUserRequestDTO.getMobile())
                        .append("|")
                        .append(systemUserRequestDTO.getFirstName())
                        .append("|")
                        .append(systemUserRequestDTO.getLastName())
                        .append("|")
                        .append(systemUserRequestDTO.getApprovalLevel())
                        .append("|")
                        .append(systemUserRequestDTO.getDeathApprovalLevel())
                        .append("|")
                        .append(normalizeCompanyCodes(systemUserRequestDTO.getCompanies()))
                        .append("|")
                        .append(systemUserRequestDTO.getStatus()).toString();

                String oldModel = new StringBuilder()
                        .append(user.getNic())
                        .append("|")
                        .append(user.getEmail())
                        .append("|")
                        .append(user.getMobile())
                        .append("|")
                        .append(user.getFirstName())
                        .append("|")
                        .append(user.getLastName())
                        .append("|")
                        .append(Objects.nonNull(user.getApprovalLevel()) ? user.getApprovalLevel().name() : null)
                        .append("|")
                        .append(Objects.nonNull(user.getDeathApprovalLevel()) ? user.getDeathApprovalLevel():null)
                        .append("|")
                        .append(normalizeCompanyCodes(user.getCompanies().stream()
                                .map(CompanyTypes::getCode)
                                .toList()))
                        .append("|")
                        .append(user.getStatus().name()).toString();

                if (oldModel.equals(newModel)) {
                    log.info("User update data not changed to {}", newModel);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1027, messageSource.getMessage(ResponseMessageUtil.USER_VALUES_NOT_CHANGING, null, locale)));
                }

                log.info("User update old audit start");
                List<String> oldAuditList = WebUserAuditMapper.mapToDTOAudit(List.of(user));
                log.info("User update old audit end");
                LinkedHashSet<CompanyTypes> companies = resolveRequestedCompanies(systemUserRequestDTO.getCompanies());
                if (companies == null) {
                    return ResponseEntity.ok().body(responseUtil.error(null, 1046,
                            messageSource.getMessage(ResponseMessageUtil.COMPANY_NOT_FOUND, null, locale)));
                }
                SystemUserMapper.mapUser(systemUserRequestDTO, user);
                user.setCompanies(companies);
                user = webUserRepository.saveAndFlush(user);
                List<String> newAuditList = WebUserAuditMapper.mapToDTOAudit(List.of(user));
                auditLogService.log(WebPage.URAM.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(), systemUserRequestDTO.getIp(), systemUserRequestDTO.getUserAgent(), gson.toJson(newAuditList), gson.toJson(oldAuditList), systemUserRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.USER_UPDATE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("User update not found {}", systemUserRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.USER_NOT_FOUND_BY_ID, new Object[]{systemUserRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> delete(SystemUserRequestDTO systemUserRequestDTO, Locale locale) {
        try {
            log.info("User delete {}", systemUserRequestDTO);
            return webUserRepository.findById(systemUserRequestDTO.getId()).map(user -> {
                log.info("User delete old audit start");
                List<String> oldAuditList = WebUserAuditMapper.mapToDTOAudit(List.of(user));
                log.info("User delete old audit end");
                user.setStatus(Status.DELETE);
                webUserRepository.saveAndFlush(user);
                List<String> newAuditList = WebUserAuditMapper.mapToDTOAudit(List.of(user));
                auditLogService.log(WebPage.URAM.name(), WebTask.DELETE.name(), AuditTask.DELETE_DATA.getDescription(), systemUserRequestDTO.getIp(), systemUserRequestDTO.getUserAgent(), gson.toJson(newAuditList), gson.toJson(oldAuditList), systemUserRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.USER_DELETE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("User delete not found {}", systemUserRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.USER_NOT_FOUND_BY_ID, new Object[]{systemUserRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> sentOTP(SystemUserRequestDTO systemUserRequestDTO, Locale locale) {
        try {
            log.info("User sentPassword {}", systemUserRequestDTO);

            return webUserRepository.findByUsername(systemUserRequestDTO.getNewUsername()).map(user -> {
                log.info("Calling OTP generate sent password reset");
                String otp = RandomGeneratorUtil.getRandom6DigitNumber();
                log.info("OTP generated  sent password reset {}", otp);

                MessageResponseDTO messageResponseDTO =
                        messageService.sendMessage(MessageType.SENT_OTP_PASSWORD, otp,null, user.getMobile());

                log.info("Sent OTP, waiting for response... sent password reset");

                if (messageResponseDTO.isSuccess()) {
                    log.info("Message sent successfully  for sent password reset {}", messageResponseDTO);

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
                            systemUserRequestDTO.getIp(),
                            systemUserRequestDTO.getUserAgent(),
                            gson.toJson(newAuditList),
                            null,
                            systemUserRequestDTO.getUsername()
                    );

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
                } else {
                    log.info("Message failed to send  for sent password reset{}", messageResponseDTO);
                    return ResponseEntity.ok().body(
                            responseUtil.error(
                                    null,
                                    1018,
                                    messageResponseDTO.getMessage()
                            )
                    );
                }
            }).orElseGet(() -> {
                log.info("User send user not found {}", systemUserRequestDTO.getNewUsername());
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

    private LinkedHashSet<CompanyTypes> resolveRequestedCompanies(List<String> companyCodes) {
        if (companyCodes == null || companyCodes.isEmpty()) {
            return null;
        }

        LinkedHashSet<String> requestedCodes = companyCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedCodes.isEmpty()) {
            return null;
        }

        List<CompanyTypes> resolvedCompanies = companyTypeRepository.findAllByCodeInAndStatus(requestedCodes, Status.ACTIVE);
        if (resolvedCompanies.size() != requestedCodes.size()) {
            return null;
        }

        Map<String, CompanyTypes> companyMap = resolvedCompanies.stream()
                .collect(Collectors.toMap(CompanyTypes::getCode, company -> company));

        LinkedHashSet<CompanyTypes> companies = new LinkedHashSet<>();
        for (String code : requestedCodes) {
            CompanyTypes company = companyMap.get(code);
            if (company == null) {
                return null;
            }
            companies.add(company);
        }
        return companies;
    }

    private String normalizeCompanyCodes(Collection<String> companyCodes) {
        if (companyCodes == null || companyCodes.isEmpty()) {
            return "";
        }
        return companyCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(String::toUpperCase)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(","));
    }

    @Transactional(readOnly = true)
    protected String validAlignCurrentUsernamePolicy(String username) {
        try {
            log.info("username align with current username policy {}", username);
            return webUsernamePolicyRepository.findUsernamePolicy()
                    .map(policy -> {
                        int charCount = StringUtil.getCharCount(username);
                        // max length check
                        if (charCount > policy.getMaxLength()) {
                            log.info("username invalid max length validation {}", username);
                            return messageSource.getMessage("val.username.max.length.invalid", new Object[]{policy.getMaxLength()}, null);
                        }

                        // min length check
                        if (charCount < policy.getMinLength()) {
                            log.info("username invalid min length validation char count {} policy min length {}", charCount, policy.getMinLength());
                            return messageSource.getMessage("val.username.min.length.invalid", new Object[]{policy.getMinLength()}, null);
                        }

                        int upperCount = StringUtil.countCharsByConditions(username, Character::isUpperCase);
                        // upper count check
                        if (upperCount < policy.getMinUpperCase()) {
                            log.info("username invalid upper case validation {}", username);
                            return messageSource.getMessage("val.username.upper.length.invalid", new Object[]{policy.getMinUpperCase()}, null);
                        }

                        int lowerCount = StringUtil.countCharsByConditions(username, Character::isLowerCase);
                        // lower count check
                        if (lowerCount < policy.getMinLowerCase()) {
                            log.info("username invalid lower case validation {}", username);
                            return messageSource.getMessage("val.username.lower.length.invalid", new Object[]{policy.getMinLowerCase()}, null);
                        }

                        int digitCount = StringUtil.countCharsByConditions(username, Character::isDigit);
                        // number count check
                        if (digitCount < policy.getMinNumbers()) {
                            log.info("username invalid min digit validation {}", username);
                            return messageSource.getMessage("val.username.number.length.invalid", new Object[]{policy.getMinNumbers()}, null);
                        }

                        int specialCharCount = StringUtil.countCharsByConditions(username, c -> !Character.isLetterOrDigit(c));
                        // special char count check
                        if (specialCharCount < policy.getMinSpecialCharacters()) {
                            log.info("username invalid special char validation {}", username);
                            return messageSource.getMessage("val.username.special.length.invalid", new Object[]{policy.getMinSpecialCharacters()}, null);
                        }
                        log.info("username success validation {}", username);
                        return "";
                    })
                    .orElseGet(() -> {
                        log.info("request username policy not found for username {} ", username);
                        return messageSource.getMessage("val.username.policy.notfound", null, null);
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}

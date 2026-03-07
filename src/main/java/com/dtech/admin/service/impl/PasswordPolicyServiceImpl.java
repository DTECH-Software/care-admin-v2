/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 12:01 PM
 * <p>
 */

package com.dtech.admin.service.impl;


import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PasswordPolicyRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.PasswordPolicyResponseDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.WebPasswordPolicyAuditMapper;
import com.dtech.admin.mapper.entityToDto.PasswordPolicyMapper;
import com.dtech.admin.repository.WebPasswordPolicyRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.PasswordPolicyService;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    @Autowired
    private final WebPasswordPolicyRepository webPasswordPolicyRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Web password policy page reference data {} ", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.PSPM.name());

            responseMap.put("privileges", privileges);
            auditLogService.log(WebPage.PSPM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.PSPM.name()}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(PasswordPolicyRequestDTO passwordPolicyRequestDTO, Locale locale) {

        try {
            log.info("Web password policy page view data {} ", passwordPolicyRequestDTO);
            return webPasswordPolicyRepository.findPasswordPolicy().map(pw -> {
                PasswordPolicyResponseDTO passwordPolicyResponseDTO = PasswordPolicyMapper.mapPasswordPolicyDetails(pw);
                List<String> newAuditList = WebPasswordPolicyAuditMapper.mapToDTOAudit(List.of(PasswordPolicyMapper.mapPasswordPolicyDetails(pw)));
                auditLogService.log(WebPage.PSPM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), passwordPolicyRequestDTO.getIp(), passwordPolicyRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, passwordPolicyRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) passwordPolicyResponseDTO, messageSource.getMessage(ResponseMessageUtil.PASSWORD_POLICY_FOUND_BY_ID, new Object[]{passwordPolicyRequestDTO.getId()}, locale)));
            }).orElseGet(() -> {
                log.info("Password policy update not found {}", passwordPolicyRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1024, messageSource.getMessage(ResponseMessageUtil.PASSWORD_POLICY_NOT_FOUND, null, locale)));
            });
        } catch (Exception e) {
            log.info(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(PasswordPolicyRequestDTO passwordPolicyRequestDTO, Locale locale) {
        try {
            log.info("Password policy update {}", passwordPolicyRequestDTO);
            return webPasswordPolicyRepository.findById(passwordPolicyRequestDTO.getId()).map(pw -> {

                String newModel = new StringBuilder()
                        .append(passwordPolicyRequestDTO.getMinUpperCase())
                        .append("|")
                        .append(passwordPolicyRequestDTO.getMinLowerCase())
                        .append("|")
                        .append(passwordPolicyRequestDTO.getMinNumbers())
                        .append("|")
                        .append(passwordPolicyRequestDTO.getMinSpecialCharacters())
                        .append("|")
                        .append(passwordPolicyRequestDTO.getMinLength())
                        .append("|")
                        .append(passwordPolicyRequestDTO.getMaxLength())
                        .append("|")
                        .append(passwordPolicyRequestDTO.getPasswordHistory())
                        .append("|")
                        .append(passwordPolicyRequestDTO.getAttemptExceedCount())
                        .append("|")
                        .append(passwordPolicyRequestDTO.getOtpExceedCount()).toString();

                String oldModel = new StringBuilder()
                        .append(pw.getMinUpperCase())
                        .append("|")
                        .append(pw.getMinLowerCase())
                        .append("|")
                        .append(pw.getMinNumbers())
                        .append("|")
                        .append(pw.getMinSpecialCharacters())
                        .append("|")
                        .append(pw.getMinLength())
                        .append("|")
                        .append(pw.getMaxLength())
                        .append("|")
                        .append(pw.getPasswordHistory())
                        .append("|")
                        .append(pw.getAttemptExceedCount())
                        .append("|")
                        .append(pw.getOtpExceedCount()).toString();

                if (oldModel.equals(newModel)) {
                    log.info("Password policy update status not changed to {}", newModel);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1023, messageSource.getMessage(ResponseMessageUtil.PASSWORD_POLICY_VALUES_NOT_CHANGING, null, locale)));
                }

                log.info("Password policy update old audit start");
                List<String> oldAuditList = WebPasswordPolicyAuditMapper.mapToDTOAudit(List.of(PasswordPolicyMapper.mapPasswordPolicyDetails(pw)));
                log.info("Password policy update old audit end");
                pw = com.dtech.admin.mapper.dtoToEntity.PasswordPolicyMapper.mapPasswordPolicy(passwordPolicyRequestDTO);
                webPasswordPolicyRepository.saveAndFlush(pw);
                List<String> newAuditList = WebPasswordPolicyAuditMapper.mapToDTOAudit(List.of(PasswordPolicyMapper.mapPasswordPolicyDetails(pw)));
                auditLogService.log(WebPage.PSPM.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(), passwordPolicyRequestDTO.getIp(), passwordPolicyRequestDTO.getUserAgent(), gson.toJson(newAuditList), gson.toJson(oldAuditList), passwordPolicyRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.PASSWORD_POLICY_UPDATE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Password policy update not found {}", passwordPolicyRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1024, messageSource.getMessage(ResponseMessageUtil.PASSWORD_POLICY_NOT_FOUND_BY_ID, new Object[]{passwordPolicyRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}

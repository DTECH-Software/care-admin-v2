/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 10:32 AM
 * <p>
 */

package com.dtech.admin.service.impl;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.UsernamePolicyRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.UsernamePolicyResponseDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.WebUsernamePolicyAuditMapper;
import com.dtech.admin.mapper.entityToDto.UsernamePolicyMapper;
import com.dtech.admin.repository.WebUsernamePolicyRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.UsernamePolicyService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class UsernamePolicyServiceImpl implements UsernamePolicyService {

    @Autowired
    private final WebUsernamePolicyRepository webUsernamePolicyRepository;

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
            log.info("Web username policy page reference data {} ", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.UNPM.name());

            responseMap.put("privileges", privileges);
            auditLogService.log(WebPage.UNPM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.UNPM.name()}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(UsernamePolicyRequestDTO usernamePolicyRequestDTO, Locale locale) {

        try {
            log.info("Web password policy page view data {} ", usernamePolicyRequestDTO);
            return webUsernamePolicyRepository.findUsernamePolicy().map(un -> {
                UsernamePolicyResponseDTO usernamePolicyResponseDTO = UsernamePolicyMapper.mapUsernamePolicyDetails(un);
                List<String> newAuditList = WebUsernamePolicyAuditMapper.mapToDTOAudit(List.of(UsernamePolicyMapper.mapUsernamePolicyDetails(un)));
                auditLogService.log(WebPage.UNPM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), usernamePolicyRequestDTO.getIp(), usernamePolicyRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, usernamePolicyRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) usernamePolicyResponseDTO, messageSource.getMessage(ResponseMessageUtil.USERNAME_POLICY_FOUND_BY_ID, new Object[]{un.getId()}, locale)));
            }).orElseGet(() -> {
                log.info("Username policy update not found {}", usernamePolicyRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1025, messageSource.getMessage(ResponseMessageUtil.USERNAME_POLICY_NOT_FOUND, null, locale)));
            });
        } catch (Exception e) {
            log.info(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(UsernamePolicyRequestDTO usernamePolicyRequestDTO, Locale locale) {
        try {
            log.info("Password policy update {}", usernamePolicyRequestDTO);
            return webUsernamePolicyRepository.findById(usernamePolicyRequestDTO.getId()).map(un -> {

                String newModel = new StringBuilder()
                        .append(usernamePolicyRequestDTO.getMinUpperCase())
                        .append("|")
                        .append(usernamePolicyRequestDTO.getMinLowerCase())
                        .append("|")
                        .append(usernamePolicyRequestDTO.getMinNumbers())
                        .append("|")
                        .append(usernamePolicyRequestDTO.getMinSpecialCharacters())
                        .append("|")
                        .append(usernamePolicyRequestDTO.getMinLength())
                        .append("|")
                        .append(usernamePolicyRequestDTO.getMaxLength()).toString();

                String oldModel = new StringBuilder()
                        .append(un.getMinUpperCase())
                        .append("|")
                        .append(un.getMinLowerCase())
                        .append("|")
                        .append(un.getMinNumbers())
                        .append("|")
                        .append(un.getMinSpecialCharacters())
                        .append("|")
                        .append(un.getMinLength())
                        .append("|")
                        .append(un.getMaxLength()).toString();

                if (oldModel.equals(newModel)) {
                    log.info("Username policy update status not changed to {}", newModel);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1026, messageSource.getMessage(ResponseMessageUtil.USERNAME_POLICY_VALUES_NOT_CHANGING, null, locale)));
                }

                log.info("Username policy update old audit start");
                List<String> oldAuditList = WebUsernamePolicyAuditMapper.mapToDTOAudit(List.of(UsernamePolicyMapper.mapUsernamePolicyDetails(un)));
                log.info("Username policy update old audit end");
                un = com.dtech.admin.mapper.dtoToEntity.UsernamePolicyMapper.mapUsernamePolicy(usernamePolicyRequestDTO);
                webUsernamePolicyRepository.saveAndFlush(un);
                List<String> newAuditList = WebUsernamePolicyAuditMapper.mapToDTOAudit(List.of(UsernamePolicyMapper.mapUsernamePolicyDetails(un)));
                auditLogService.log(WebPage.UNPM.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(), usernamePolicyRequestDTO.getIp(), usernamePolicyRequestDTO.getUserAgent(), gson.toJson(newAuditList), gson.toJson(oldAuditList), usernamePolicyRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.USERNAME_POLICY_UPDATE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Username policy update not found {}", usernamePolicyRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1025, messageSource.getMessage(ResponseMessageUtil.USERNAME_POLICY_NOT_FOUND_BY_ID, new Object[]{usernamePolicyRequestDTO.getUsername()}, locale)));  });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}

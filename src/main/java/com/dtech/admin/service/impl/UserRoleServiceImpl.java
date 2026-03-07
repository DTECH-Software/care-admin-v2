/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 12:01 PM
 * <p>
 */

package com.dtech.admin.service.impl;


import com.dtech.admin.dto.CommonResponseDTO;
import com.dtech.admin.dto.search.CommonSearchDTO;
import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.UserRoleRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.WebUserRoleAuditMapper;
import com.dtech.admin.mapper.entityToDto.UserRoleMapper;
import com.dtech.admin.model.WebUserRole;
import com.dtech.admin.repository.WebUserRoleRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.UserRoleService;
import com.dtech.admin.specifications.UserRoleSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    @Autowired
    private final WebUserRoleRepository webUserRoleRepository;

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
            log.info("User role page reference data {} ", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.USRM.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Status.values())
                    .filter(status -> !Status.DELETE.name().equals(status.name())).map(st -> {
                        return new SimpleBaseDTO(st.name(), st.getDescription());
                    }).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            auditLogService.log(WebPage.USRM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.USRM.name()}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("User role filter list {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            Page<com.dtech.admin.model.WebUserRole> webUserRoles = Objects.nonNull(paginationRequest.getSearch()) ?
                    webUserRoleRepository.findAll(UserRoleSpecification.getSpecification(paginationRequest.getSearch()), pageable) :
                    webUserRoleRepository.findAll(UserRoleSpecification.getSpecification(), pageable);
            log.info("User role filter records {}", webUserRoles);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    webUserRoleRepository.count(UserRoleSpecification.getSpecification(paginationRequest.getSearch())) :
                    webUserRoleRepository.count(UserRoleSpecification.getSpecification());
            log.info("User role filter records map start");
            List<CommonResponseDTO> responseDTOList = webUserRoles.stream()
                    .map(UserRoleMapper::mapUserRoleDetails).toList();
            log.info("User role filter records map finish");
            List<String> newAuditList = WebUserRoleAuditMapper.mapToDTOAudit(responseDTOList);
            auditLogService.log(WebPage.USRM.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<CommonResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.USER_ROLE_FILTER_LIST_SUCCESS,
                            null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> add(UserRoleRequestDTO userRoleRequestDTO, Locale locale) {
        try {
            log.info("User role add {}", userRoleRequestDTO);

            boolean exists = webUserRoleRepository.existsByCodeAndStatusIn(userRoleRequestDTO.getCode(), List.of(Status.ACTIVE, Status.INACTIVE));

            if (exists) {
                log.info("User role code {} already exists", userRoleRequestDTO.getCode());
                return ResponseEntity.ok().body(
                        responseUtil.error(
                                null,
                                1022,
                                messageSource.getMessage(
                                        ResponseMessageUtil.USER_ROLE_CODE_ALREADY_EXISTS,
                                        new Object[]{userRoleRequestDTO.getCode()},
                                        locale
                                )
                        )
                );
            }

            WebUserRole webUserRole = com.dtech.admin.mapper.dtoToEntity.UserRoleMapper.mapUserRole(userRoleRequestDTO);
            webUserRoleRepository.saveAndFlush(webUserRole);
            log.info("User role added {}", webUserRole);
            List<String> newAuditList = WebUserRoleAuditMapper.mapToDTOAudit(List.of(UserRoleMapper.mapUserRoleDetails(webUserRole)));
            auditLogService.log(
                    WebPage.USRM.name(),
                    WebTask.ADD.name(),
                    AuditTask.ADD_DATA.getDescription(),
                    userRoleRequestDTO.getIp(),
                    userRoleRequestDTO.getUserAgent(),
                    gson.toJson(newAuditList),
                    null,
                    userRoleRequestDTO.getUsername()
            );

            return ResponseEntity.ok().body(
                    responseUtil.success(
                            null,
                            messageSource.getMessage(
                                    ResponseMessageUtil.USER_ROLE_ADDED_SUCCESS,
                                    new Object[]{userRoleRequestDTO.getCode()},
                                    locale
                            )
                    )
            );

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(UserRoleRequestDTO userRoleRequestDTO, Locale locale) {
        try {
            log.info("User role view {}", userRoleRequestDTO);
            return webUserRoleRepository.findById(userRoleRequestDTO.getId()).map(ur -> {
                CommonResponseDTO section = UserRoleMapper.mapUserRoleDetails(ur);
                List<String> newAuditList = WebUserRoleAuditMapper.mapToDTOAudit(List.of(section));
                auditLogService.log(WebPage.USRM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), userRoleRequestDTO.getIp(), userRoleRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, userRoleRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) section, messageSource.getMessage(ResponseMessageUtil.USER_ROLE_FOUND_BY_ID, new Object[]{userRoleRequestDTO.getId()}, locale)));
            }).orElseGet(() -> {
                log.info("User role view not found {}", userRoleRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1020, messageSource.getMessage(ResponseMessageUtil.USER_ROLE_NOT_FOUND_BY_ID, new Object[]{userRoleRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.info(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(UserRoleRequestDTO userRoleRequestDTO, Locale locale) {
        try {
            log.info("User role update {}", userRoleRequestDTO);
            return webUserRoleRepository.findById(userRoleRequestDTO.getId()).map(ur -> {

                String newModel = new StringBuilder()
                        .append(userRoleRequestDTO.getDescription())
                        .append("|")
                        .append(userRoleRequestDTO.getStatus()).toString();

                String oldModel = new StringBuilder()
                        .append(ur.getDescription())
                        .append("|")
                        .append(ur.getStatus().name()).toString();

                if (oldModel.equals(newModel)) {
                    log.info("User role update status not changed to {}", newModel);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1021, messageSource.getMessage(ResponseMessageUtil.USER_ROLE_VALUES_NOT_CHANGING, null, locale)));
                }
                log.info("User role update old audit start");
                List<String> oldAuditList = WebUserRoleAuditMapper.mapToDTOAudit(List.of(UserRoleMapper.mapUserRoleDetails(ur)));
                log.info("User role update old audit end");
                ur.setStatus(Status.valueOf(userRoleRequestDTO.getStatus()));
                ur.setDescription(userRoleRequestDTO.getDescription());
                ur = webUserRoleRepository.saveAndFlush(ur);
                List<String> newAuditList = WebUserRoleAuditMapper.mapToDTOAudit(List.of(UserRoleMapper.mapUserRoleDetails(ur)));
                auditLogService.log(WebPage.USRM.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(), userRoleRequestDTO.getIp(), userRoleRequestDTO.getUserAgent(), gson.toJson(newAuditList), gson.toJson(oldAuditList), userRoleRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.USER_ROLE_UPDATE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("User role update not found {}", userRoleRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1020, messageSource.getMessage(ResponseMessageUtil.USER_ROLE_NOT_FOUND_BY_ID, new Object[]{userRoleRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> delete(UserRoleRequestDTO userRoleRequestDTO, Locale locale) {
        try {
            log.info("User role delete {}", userRoleRequestDTO);

            return webUserRoleRepository.findById(userRoleRequestDTO.getId()).map(ur -> {
                log.info("User role delete old audit start");
                List<String> oldAuditList = WebUserRoleAuditMapper.mapToDTOAudit(List.of(UserRoleMapper.mapUserRoleDetails(ur)));
                log.info("User role delete old audit end");
                ur.setStatus(Status.DELETE);
                webUserRoleRepository.saveAndFlush(ur);
                List<String> newAuditList = WebUserRoleAuditMapper.mapToDTOAudit(List.of(UserRoleMapper.mapUserRoleDetails(ur)));
                auditLogService.log(WebPage.USRM.name(), WebTask.DELETE.name(), AuditTask.DELETE_DATA.getDescription(), userRoleRequestDTO.getIp(), userRoleRequestDTO.getUserAgent(), gson.toJson(newAuditList), gson.toJson(oldAuditList), userRoleRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.USER_ROLE_DELETE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("User role delete not found {}", userRoleRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1020, messageSource.getMessage(ResponseMessageUtil.USER_ROLE_NOT_FOUND_BY_ID, new Object[]{userRoleRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.info(e);
            throw e;
        }
    }
}

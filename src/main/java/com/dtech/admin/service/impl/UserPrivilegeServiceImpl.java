/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 11:56 AM
 * <p>
 */

package com.dtech.admin.service.impl;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.AssignedUnAssignedTaskRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AssignedUnAssignedTaskResponseDTO;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.WebUserRolePageTaskAuditMapper;
import com.dtech.admin.model.WebUserRolePageTask;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.UserPrivilegeService;
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
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserPrivilegeServiceImpl implements UserPrivilegeService {

    @Autowired
    private final WebUserRoleRepository webUserRoleRepository;

    @Autowired
    private final WebSectionRepository webSectionRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final WebPageRepository webPageRepository;

    @Autowired
    private final WebUserRolePageTaskRepository webUserRolePageTaskRepository;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final WebTaskRepository webTaskRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("UserPrivilege page reference data {} ", channelRequestDTO);
            Map<String,Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.getPrivileges(channelRequestDTO.getUsername(), WebPage.URPM.name());
            List<SimpleBaseDTO> userRole = webUserRoleRepository.findAllByWebUserRole(Status.ACTIVE);
            List<SimpleBaseDTO> section = webSectionRepository.findAllBySection(Status.ACTIVE);
            Map<String, Object> underSection = section.stream()
                    .collect(Collectors.toMap(
                            SimpleBaseDTO::getCode,
                            se -> webPageRepository.findAllPagesBySection(Status.ACTIVE, se.getCode())
                    ));
            responseMap.put("privileges", privileges);
            responseMap.put("userRole", userRole);
            responseMap.put("section", section);
            responseMap.put("page", underSection);
            auditLogService.log(WebPage.URPM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(),channelRequestDTO.getIp(),channelRequestDTO.getUserAgent(),gson.toJson(responseMap),null,channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.URPM.name()}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getTaskByPageAndUserRole(AssignedUnAssignedTaskRequestDTO assignedUnAssignedTaskRequestDTO, Locale locale) {
        try {
            log.info("UserPrivilege page get task by page and user role {} ", assignedUnAssignedTaskRequestDTO);

            return webUserRoleRepository.findByCodeAndStatus(assignedUnAssignedTaskRequestDTO.getUserRole(),Status.ACTIVE).map(role -> {
              return webPageRepository.findByCodeAndStatus(assignedUnAssignedTaskRequestDTO.getPage(),Status.ACTIVE).map(page -> {
                  List<com.dtech.admin.model.WebPageTask> allPageTask = page.getWebPageTasks();
                  List<WebUserRolePageTask> existingAssignedTask = webUserRolePageTaskRepository.findAllByRoleAndPage(role, page);

                  Set<String> assignedTaskCods =  existingAssignedTask.stream().map(t -> t.getTask().getCode()).collect(Collectors.toSet());

                  List<SimpleBaseDTO> assignedTask = allPageTask.stream()
                          .filter(task -> assignedTaskCods.contains(task.getWebTask().getCode()))
                          .map(task -> new SimpleBaseDTO(task.getWebTask().getCode(),task.getWebTask().getDescription()))
                          .toList();

                  List<SimpleBaseDTO> unAssigned = allPageTask.stream()
                          .filter(task -> !assignedTaskCods.contains(task.getWebTask().getCode()))
                          .map(task -> new SimpleBaseDTO(task.getWebTask().getCode(),task.getWebTask().getDescription()))
                          .toList();

                  AssignedUnAssignedTaskResponseDTO taskResponseDTO = AssignedUnAssignedTaskResponseDTO.builder()
                          .assignedTask(assignedTask)
                          .unAssignedTask(unAssigned)
                          .build();
                  auditLogService.log(WebPage.URPM.name(), WebTask.REF_DATA.name(), AuditTask.ASSIGNED_UNASSIGNED_TASK_RETRIEVED.getDescription(), assignedUnAssignedTaskRequestDTO.getIp(), assignedUnAssignedTaskRequestDTO.getUserAgent(),gson.toJson(taskResponseDTO),null, assignedUnAssignedTaskRequestDTO.getUsername());
                  return ResponseEntity.ok().body(responseUtil.success((Object) taskResponseDTO, messageSource.getMessage(ResponseMessageUtil.ASSIGNED_UNASSIGNED_TASK_RETRIEVED_SUCCESS, null, locale)));
              }).orElseGet(() -> {
                    log.info("UserPrivilege page not found or inactive {} ", assignedUnAssignedTaskRequestDTO.getPage());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1008, messageSource.getMessage(ResponseMessageUtil.WEB_PAGE_NOT_FOUND_OR_INACTIVE, null, locale)));
                });
            }).orElseGet(() -> {
                log.info("UserPrivilege user not found or inactive {}", assignedUnAssignedTaskRequestDTO.getUserRole());
                return ResponseEntity.ok().body(responseUtil.error(null, 1007, messageSource.getMessage(ResponseMessageUtil.WEB_USER_ROLE_NOT_FOUND_OR_INACTIVE, null, locale)));
            });

        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> assignedPrivileges(AssignedUnAssignedTaskRequestDTO assignedUnAssignedTaskRequestDTO, Locale locale) {
        try {
            log.info("UserPrivilege page assigned privileges {} ", assignedUnAssignedTaskRequestDTO);

            return webUserRoleRepository.findByCodeAndStatus(assignedUnAssignedTaskRequestDTO.getUserRole(),Status.ACTIVE).map(role -> {
                return webPageRepository.findByCodeAndStatus(assignedUnAssignedTaskRequestDTO.getPage(),Status.ACTIVE).map(page -> {

                    List<com.dtech.admin.model.WebTask> tasksToAdd = new ArrayList<>();
                    List<String> missingTaskCodes = new ArrayList<>();

                    for (String code : assignedUnAssignedTaskRequestDTO.getAssignedTask()) {
                        Optional<com.dtech.admin.model.WebTask> optionalWebTask = webTaskRepository.findByCode(code);

                        if (optionalWebTask.isPresent()) {
                            tasksToAdd.add(optionalWebTask.get());
                        } else {
                            missingTaskCodes.add(code);
                        }
                    }

                    if (!missingTaskCodes.isEmpty()) {
                        log.info("Some task code not found or inactive");
                        return ResponseEntity.ok().body(responseUtil.error(
                                null,
                                1009,
                                messageSource.getMessage(
                                        ResponseMessageUtil.FOLLOWING_TASK_NOT_FOUND_OR_INACTIVE,
                                        new Object[]{String.join(", ", missingTaskCodes)},
                                        locale
                                )
                        ));
                    }

                    List<WebUserRolePageTask> oldTask = webUserRolePageTaskRepository.findAllByRoleAndPage(role, page);
                    webUserRolePageTaskRepository.deleteAllByRoleAndPage(role,page);
                    List<WebUserRolePageTask> newTask = tasksToAdd.stream().map(task -> {
                        WebUserRolePageTask webUserRolePageTask = new WebUserRolePageTask();
                        webUserRolePageTask.setRole(role);
                        webUserRolePageTask.setPage(page);
                        webUserRolePageTask.setTask(task);
                        return webUserRolePageTask;
                    }).toList();
                    webUserRolePageTaskRepository.saveAllAndFlush(newTask);
                    log.info("Audit mapper started successfully");
                    List<String> oldAuditList = WebUserRolePageTaskAuditMapper.mapToDTOAudit(oldTask);
                    List<String> newAuditList = WebUserRolePageTaskAuditMapper.mapToDTOAudit(newTask);
                    log.info("Audit mapper end successfully");
                    auditLogService.log(WebPage.URPM.name(), WebTask.UPDATE.name(), AuditTask.TASK_ASSIGNED_SUCCESS.getDescription(), assignedUnAssignedTaskRequestDTO.getIp(), assignedUnAssignedTaskRequestDTO.getUserAgent(),gson.toJson(newAuditList),gson.toJson(oldAuditList), assignedUnAssignedTaskRequestDTO.getUsername());
                    return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.PRIVILEGE_ASSIGNED_SUCCESS, null, locale)));
                }).orElseGet(() -> {
                    log.info("UserPrivilege assigned page not found or inactive {} ", assignedUnAssignedTaskRequestDTO.getPage());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1008, messageSource.getMessage(ResponseMessageUtil.WEB_PAGE_NOT_FOUND_OR_INACTIVE, null, locale)));
                });
            }).orElseGet(() -> {
                log.info("UserPrivilege assigned user not found or inactive {}", assignedUnAssignedTaskRequestDTO.getUserRole());
                return ResponseEntity.ok().body(responseUtil.error(null, 1007, messageSource.getMessage(ResponseMessageUtil.WEB_USER_ROLE_NOT_FOUND_OR_INACTIVE, null, locale)));
            });

        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}

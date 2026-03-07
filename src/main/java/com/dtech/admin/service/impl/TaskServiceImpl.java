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
import com.dtech.admin.dto.request.TaskRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.WebTaskAuditMapper;
import com.dtech.admin.mapper.entityToDto.TaskMapper;
import com.dtech.admin.repository.WebTaskRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.TaskService;
import com.dtech.admin.specifications.TaskSpecification;
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
public class TaskServiceImpl implements TaskService {

    @Autowired
    private final WebTaskRepository webTaskRepository;

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
            log.info("Task page reference data {} ", channelRequestDTO);
            Map<String,Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.TASM.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Status.values())
                    .filter(status -> !Status.DELETE.name().equals(status.name())).map(st -> {
                        return new SimpleBaseDTO(st.name(), st.getDescription());
                    }).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            auditLogService.log(WebPage.TASM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(),channelRequestDTO.getIp(),channelRequestDTO.getUserAgent(),gson.toJson(responseMap),null,channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.TASM.name()}, locale)));
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Task filter list {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            Page<com.dtech.admin.model.WebTask> webPages = Objects.nonNull(paginationRequest.getSearch()) ?
                    webTaskRepository.findAll(TaskSpecification.getSpecification(paginationRequest.getSearch()), pageable) :
                    webTaskRepository.findAll(TaskSpecification.getSpecification(), pageable);
            log.info("Task filter records {}", webPages);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    webTaskRepository.count(TaskSpecification.getSpecification(paginationRequest.getSearch())) :
                    webTaskRepository.count(TaskSpecification.getSpecification());
            log.info("Task filter records map start");
            List<CommonResponseDTO> responseDTOList = webPages.stream()
                    .map(TaskMapper::mapTaskDetails).toList();
            log.info("Task filter records map finish");
            List<String> newAuditList = WebTaskAuditMapper.mapToDTOAudit(responseDTOList);
            auditLogService.log(WebPage.TASM.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<CommonResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.TASK_FILTER_LIST_SUCCESS,
                            null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(TaskRequestDTO taskRequestDTO, Locale locale) {
        try {
            log.info("Task view {}", taskRequestDTO);
            return webTaskRepository.findById(taskRequestDTO.getId()).map(sec -> {
                CommonResponseDTO section = TaskMapper.mapTaskDetails(sec);
                List<String> newAuditList = WebTaskAuditMapper.mapToDTOAudit(List.of(section));
                auditLogService.log(WebPage.TASM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), taskRequestDTO.getIp(), taskRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, taskRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) section, messageSource.getMessage(ResponseMessageUtil.TASK_FOUND_BY_ID, new Object[]{taskRequestDTO.getId()}, locale)));
            }).orElseGet(() -> {
                log.info("Task view not found {}", taskRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.TASK_NOT_FOUND_BY_ID, new Object[]{taskRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.info(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(TaskRequestDTO taskRequestDTO, Locale locale) {
        try {
            log.info("Task update {}", taskRequestDTO);
            return webTaskRepository.findById(taskRequestDTO.getId()).map(task -> {

                if(task.getStatus().name().equals(taskRequestDTO.getStatus())){
                    log.info("Task update status not changed to {}", taskRequestDTO.getStatus());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1015, messageSource.getMessage(ResponseMessageUtil.TASK_STATUS_NOT_CHANGING, null, locale)));
                }
                log.info("Task update old audit start");
                List<String> oldAuditList = WebTaskAuditMapper.mapToDTOAudit(List.of(TaskMapper.mapTaskDetails(task)));
                log.info("Task update old audit end");
                task.setStatus(Status.valueOf(taskRequestDTO.getStatus()));
                webTaskRepository.saveAndFlush(task);
                List<String> newAuditList = WebTaskAuditMapper.mapToDTOAudit(List.of(TaskMapper.mapTaskDetails(task)));
                auditLogService.log(WebPage.TASM.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(), taskRequestDTO.getIp(), taskRequestDTO.getUserAgent(), gson.toJson(newAuditList),  gson.toJson(oldAuditList), taskRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.TASK_UPDATE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Task update not found {}", taskRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.TASK_NOT_FOUND_BY_ID, new Object[]{taskRequestDTO.getId()}, locale)));
            });
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> delete(TaskRequestDTO taskRequestDTO, Locale locale) {
        try {
            log.info("Task delete {}", taskRequestDTO);

            return webTaskRepository.findById(taskRequestDTO.getId()).map(task -> {
                log.info("Task update old audit start");
                List<String> oldAuditList = WebTaskAuditMapper.mapToDTOAudit(List.of(TaskMapper.mapTaskDetails(task)));
                log.info("Task update old audit end");
                task.setStatus(Status.DELETE);
                webTaskRepository.saveAndFlush(task);
                List<String> newAuditList = WebTaskAuditMapper.mapToDTOAudit(List.of(TaskMapper.mapTaskDetails(task)));
                auditLogService.log(WebPage.TASM.name(), WebTask.DELETE.name(), AuditTask.DELETE_DATA.getDescription(), taskRequestDTO.getIp(), taskRequestDTO.getUserAgent(), gson.toJson(newAuditList),  gson.toJson(oldAuditList), taskRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.TASK_DELETE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Task delete not found {}", taskRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.TASK_NOT_FOUND_BY_ID, new Object[]{taskRequestDTO.getId()}, locale)));
            });
        }catch (Exception e) {
            log.info(e);
            throw e;
        }
    }
}

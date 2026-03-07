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
import com.dtech.admin.dto.request.PageRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.WebPageAuditMapper;
import com.dtech.admin.mapper.entityToDto.PageMapper;
import com.dtech.admin.repository.WebPageRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.PageService;
import com.dtech.admin.specifications.PageSpecification;
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
public class PageServiceImpl implements PageService {

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
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Page page reference data {} ", channelRequestDTO);
            Map<String,Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.PAGM.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Status.values())
                    .filter(status -> !Status.DELETE.name().equals(status.name())).map(st -> {
                        return new SimpleBaseDTO(st.name(), st.getDescription());
                    }).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            auditLogService.log(WebPage.PAGM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(),channelRequestDTO.getIp(),channelRequestDTO.getUserAgent(),gson.toJson(responseMap),null,channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.PAGM.name()}, locale)));

        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Page filter list {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            Page<com.dtech.admin.model.WebPage> webPages = Objects.nonNull(paginationRequest.getSearch()) ?
                    webPageRepository.findAll(PageSpecification.getSpecification(paginationRequest.getSearch()), pageable) :
                    webPageRepository.findAll(PageSpecification.getSpecification(), pageable);
            log.info("Page filter records {}", webPages);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    webPageRepository.count(PageSpecification.getSpecification(paginationRequest.getSearch())) :
                    webPageRepository.count(PageSpecification.getSpecification());
            log.info("Page filter records map start");
            List<CommonResponseDTO> responseDTOList = webPages.stream()
                    .map(PageMapper::mapPageDetails).toList();
            log.info("Page filter records map finish");
            List<String> newAuditList = WebPageAuditMapper.mapToDTOAudit(responseDTOList);
            auditLogService.log(WebPage.PAGM.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<CommonResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.PAGE_FILTER_LIST_SUCCESS,
                            null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(PageRequestDTO pageRequestDTO, Locale locale) {
        try {
            log.info("Page view {}", pageRequestDTO);
            return webPageRepository.findById(pageRequestDTO.getId()).map(sec -> {
                CommonResponseDTO section = PageMapper.mapPageDetails(sec);
                List<String> newAuditList = WebPageAuditMapper.mapToDTOAudit(List.of(section));
                auditLogService.log(WebPage.PAGM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), pageRequestDTO.getIp(), pageRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, pageRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) section, messageSource.getMessage(ResponseMessageUtil.PAGE_FOUND_BY_ID, new Object[]{pageRequestDTO.getId()}, locale)));
            }).orElseGet(() -> {
                log.info("Page view not found {}", pageRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1012, messageSource.getMessage(ResponseMessageUtil.PAGE_NOT_FOUND_BY_ID, new Object[]{pageRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.info(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(PageRequestDTO pageRequestDTO, Locale locale) {
        try {
            log.info("Page update {}", pageRequestDTO);
            return webPageRepository.findById(pageRequestDTO.getId()).map(page -> {

                if(page.getStatus().name().equals(pageRequestDTO.getStatus())){
                    log.info("Page update status not changed to {}", pageRequestDTO.getStatus());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1013, messageSource.getMessage(ResponseMessageUtil.PAGE_STATUS_NOT_CHANGING, null, locale)));
                }
                log.info("Page update old audit start");
                List<String> oldAuditList = WebPageAuditMapper.mapToDTOAudit(List.of(PageMapper.mapPageDetails(page)));
                log.info("Page update old audit end");
                page.setStatus(Status.valueOf(pageRequestDTO.getStatus()));
                webPageRepository.saveAndFlush(page);
                List<String> newAuditList = WebPageAuditMapper.mapToDTOAudit(List.of(PageMapper.mapPageDetails(page)));
                auditLogService.log(WebPage.PAGM.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(), pageRequestDTO.getIp(), pageRequestDTO.getUserAgent(), gson.toJson(newAuditList),  gson.toJson(oldAuditList), pageRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.PAGE_UPDATE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Page update not found {}", pageRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1012, messageSource.getMessage(ResponseMessageUtil.PAGE_NOT_FOUND_BY_ID, new Object[]{pageRequestDTO.getId()}, locale)));
            });
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> delete(PageRequestDTO pageRequestDTO, Locale locale) {
        try {
            log.info("Page delete {}", pageRequestDTO);

            return webPageRepository.findById(pageRequestDTO.getId()).map(page -> {
                log.info("Page update old audit start");
                List<String> oldAuditList = WebPageAuditMapper.mapToDTOAudit(List.of(PageMapper.mapPageDetails(page)));
                log.info("Page update old audit end");
                page.setStatus(Status.DELETE);
                webPageRepository.saveAndFlush(page);
                List<String> newAuditList = WebPageAuditMapper.mapToDTOAudit(List.of(PageMapper.mapPageDetails(page)));
                auditLogService.log(WebPage.PAGM.name(), WebTask.DELETE.name(), AuditTask.DELETE_DATA.getDescription(), pageRequestDTO.getIp(), pageRequestDTO.getUserAgent(), gson.toJson(newAuditList),  gson.toJson(oldAuditList), pageRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.PAGE_DELETE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Page delete not found {}", pageRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1012, messageSource.getMessage(ResponseMessageUtil.PAGE_NOT_FOUND_BY_ID, new Object[]{pageRequestDTO.getId()}, locale)));
            });
        }catch (Exception e) {
            log.info(e);
            throw e;
        }
    }
}

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
import com.dtech.admin.dto.request.SectionRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.entityToDto.SectionMapper;
import com.dtech.admin.mapper.audit.WebSectionAuditMapper;
import com.dtech.admin.model.WebSection;
import com.dtech.admin.repository.WebSectionRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.SectionService;
import com.dtech.admin.specifications.SectionSpecification;
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
public class SectionServiceImpl implements SectionService {

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
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Section page reference data {} ", channelRequestDTO);
            Map<String,Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.SECM.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Status.values())
                    .filter(status -> !Status.DELETE.name().equals(status.name())).map(st -> {
                return new SimpleBaseDTO(st.name(), st.getDescription());
            }).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            auditLogService.log(WebPage.SECM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(),channelRequestDTO.getIp(),channelRequestDTO.getUserAgent(),gson.toJson(responseMap),null,channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.SECM.name()}, locale)));

        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Section filter list {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            Page<WebSection> webSections = Objects.nonNull(paginationRequest.getSearch()) ?
                    webSectionRepository.findAll(SectionSpecification.getSpecification(paginationRequest.getSearch()), pageable) :
                    webSectionRepository.findAll(SectionSpecification.getSpecification(), pageable);
            log.info("Section filter records {}", webSections);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    webSectionRepository.count(SectionSpecification.getSpecification(paginationRequest.getSearch())) :
                    webSectionRepository.count(SectionSpecification.getSpecification());
            log.info("Section filter records map start");
            List<CommonResponseDTO> responseDTOList = webSections.stream()
                    .map(SectionMapper::mapSectionDetails).toList();
            log.info("Section filter records map finish");
            List<String> newAuditList = WebSectionAuditMapper.mapToDTOAudit(responseDTOList);
            auditLogService.log(WebPage.SECM.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<CommonResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.SECTION_FILTER_LIST_SUCCESS,
                            null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(SectionRequestDTO sectionRequestDTO, Locale locale) {
        try {
            log.info("Section view {}", sectionRequestDTO);
            return webSectionRepository.findById(sectionRequestDTO.getId()).map(sec -> {
                CommonResponseDTO section = SectionMapper.mapSectionDetails(sec);
                List<String> newAuditList = WebSectionAuditMapper.mapToDTOAudit(List.of(section));
                auditLogService.log(WebPage.SECM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), sectionRequestDTO.getIp(), sectionRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, sectionRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) section, messageSource.getMessage(ResponseMessageUtil.SECTION_FOUND_BY_ID, new Object[]{sectionRequestDTO.getId()}, locale)));
            }).orElseGet(() -> {
                log.info("Section view not found {}", sectionRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.SECTION_NOT_FOUND_BY_ID, new Object[]{sectionRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.info(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(SectionRequestDTO sectionRequestDTO, Locale locale) {
        try {
            log.info("Section update {}", sectionRequestDTO);
            return webSectionRepository.findById(sectionRequestDTO.getId()).map(sec -> {

                if(sec.getStatus().name().equals(sectionRequestDTO.getStatus())){
                    log.info("Section update status not changed to {}", sectionRequestDTO.getStatus());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1011, messageSource.getMessage(ResponseMessageUtil.SECTION_STATUS_NOT_CHANGING, null, locale)));
                }
                log.info("Section update old audit start");
                List<String> oldAuditList = WebSectionAuditMapper.mapToDTOAudit(List.of(SectionMapper.mapSectionDetails(sec)));
                log.info("Section update old audit end");
                sec.setStatus(Status.valueOf(sectionRequestDTO.getStatus()));
                webSectionRepository.saveAndFlush(sec);
                List<String> newAuditList = WebSectionAuditMapper.mapToDTOAudit(List.of(SectionMapper.mapSectionDetails(sec)));
                auditLogService.log(WebPage.SECM.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(), sectionRequestDTO.getIp(), sectionRequestDTO.getUserAgent(), gson.toJson(newAuditList),  gson.toJson(oldAuditList), sectionRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.SECTION_UPDATE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Section update not found {}", sectionRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.SECTION_NOT_FOUND_BY_ID, new Object[]{sectionRequestDTO.getId()}, locale)));
            });
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> delete(SectionRequestDTO sectionRequestDTO, Locale locale) {
        try {
            log.info("Section delete {}", sectionRequestDTO);

            return webSectionRepository.findById(sectionRequestDTO.getId()).map(sec -> {
                log.info("Section update old audit start");
                List<String> oldAuditList = WebSectionAuditMapper.mapToDTOAudit(List.of(SectionMapper.mapSectionDetails(sec)));
                log.info("Section update old audit end");
                sec.setStatus(Status.DELETE);
                webSectionRepository.saveAndFlush(sec);
                List<String> newAuditList = WebSectionAuditMapper.mapToDTOAudit(List.of(SectionMapper.mapSectionDetails(sec)));
                auditLogService.log(WebPage.SECM.name(), WebTask.DELETE.name(), AuditTask.DELETE_DATA.getDescription(), sectionRequestDTO.getIp(), sectionRequestDTO.getUserAgent(), gson.toJson(newAuditList),  gson.toJson(oldAuditList), sectionRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.SECTION_DELETE_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Section delete not found {}", sectionRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.SECTION_NOT_FOUND_BY_ID, new Object[]{sectionRequestDTO.getId()}, locale)));
            });

        }catch (Exception e) {
            log.info(e);
            throw e;
        }
    }
}

package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuditLogResponseDTO;
import com.dtech.admin.dto.search.AuditLogSearchDTO;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.model.AuditLog;
import com.dtech.admin.repository.AuditLogRepository;
import com.dtech.admin.repository.WebPageRepository;
import com.dtech.admin.repository.WebTaskRepository;
import com.dtech.admin.service.AllActivityAuditService;
import com.dtech.admin.specifications.AuditLogSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AllActivityAuditServiceImpl implements AllActivityAuditService {
    private static final String PAGE_CODE = WebPage.ADIT_ALL.name();
    private final AuditLogRepository auditLogRepository;
    private final WebPageRepository webPageRepository;
    private final WebTaskRepository webTaskRepository;
    private final CommonPrivilegeGetter commonPrivilegeGetter;
    private final ResponseUtil responseUtil;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO request, Locale locale) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("privileges", commonPrivilegeGetter.getPrivileges(request.getUsername(), PAGE_CODE));
        data.put("sources", List.of(new SimpleBaseDTO("ALL", "All"),
                new SimpleBaseDTO("WECARE_ADMIN", "WeCare Admin"),
                new SimpleBaseDTO("WECARE_APP", "WeCare App")));
        data.put("pages", webPageRepository.findAll().stream()
                .map(p -> new SimpleBaseDTO(p.getCode(), p.getDescription())).toList());
        data.put("tasks", webTaskRepository.findAll().stream()
                .map(t -> new SimpleBaseDTO(t.getCode(), t.getDescription())).toList());
        return ResponseEntity.ok(responseUtil.success(data, "Audit log reference data retrieved successfully"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<AuditLogSearchDTO> request, Locale locale) {
        normalizeSort(request);
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecification.getSpecification(request.getSearch()), PaginationUtil.getPageable(request));
        List<AuditLogResponseDTO> rows = page.stream().map(this::map).toList();
        return ResponseEntity.ok(responseUtil.success(
                new PagingResult<>(rows, rows.size(), page.getTotalElements()),
                "Audit logs retrieved successfully"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> view(Long id, Locale locale) {
        return auditLogRepository.findById(id)
                .map(log -> ResponseEntity.ok(responseUtil.success((Object) map(log), "Audit log retrieved successfully")))
                .orElseGet(() -> ResponseEntity.ok(responseUtil.error(null, 1043, "Audit log not found")));
    }

    private AuditLogResponseDTO map(AuditLog log) {
        return AuditLogResponseDTO.builder()
                .id(log.getId()).dateTime(log.getCreatedDate()).source(log.getSource())
                .pageCode(log.getPage() == null ? null : log.getPage().getCode())
                .pageDescription(log.getPage() == null ? null : log.getPage().getDescription())
                .taskCode(log.getTask() == null ? null : log.getTask().getCode())
                .taskDescription(log.getTaskDescription()).username(log.getCreatedBy())
                .ipAddress(log.getIpAddress()).userAgent(log.getUserAgent())
                .oldValue(log.getOldValue()).newValue(log.getNewValue()).build();
    }

    private void normalizeSort(PaginationRequest<AuditLogSearchDTO> request) {
        if (!List.of("id", "createdDate", "source", "createdBy", "ipAddress").contains(request.getSortColumn()))
            request.setSortColumn("createdDate");
    }
}

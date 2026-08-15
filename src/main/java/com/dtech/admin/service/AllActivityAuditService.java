package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.AuditLogSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface AllActivityAuditService {
    ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO request, Locale locale);
    ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<AuditLogSearchDTO> request, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(Long id, String username, Locale locale);
}

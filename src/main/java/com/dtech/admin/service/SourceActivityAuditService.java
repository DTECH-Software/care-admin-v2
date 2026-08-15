package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.AuditLogSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface SourceActivityAuditService {
    ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO request, String source,
                                                         String pageCode, Locale locale);
    ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<AuditLogSearchDTO> request, String source,
                                               String pageCode, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(Long id, String username, String source,
                                             String pageCode, Locale locale);
}

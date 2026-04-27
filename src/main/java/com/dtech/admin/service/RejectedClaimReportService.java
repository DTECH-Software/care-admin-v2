package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.RejectedClaimReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface RejectedClaimReportService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<RejectedClaimReportSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<byte[]> export(PaginationRequest<RejectedClaimReportSearchDTO> paginationRequest, Locale locale);
}

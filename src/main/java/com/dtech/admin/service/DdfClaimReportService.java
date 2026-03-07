package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DdfClaimReportRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.DdfClaimReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface DdfClaimReportService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<DdfClaimReportSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> view(DdfClaimReportRequestDTO requestDTO, Locale locale);

    ResponseEntity<byte[]> export(PaginationRequest<DdfClaimReportSearchDTO> paginationRequest, Locale locale);
}

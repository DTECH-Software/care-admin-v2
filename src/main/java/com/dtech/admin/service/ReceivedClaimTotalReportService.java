package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ReceivedClaimTotalReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ReceivedClaimTotalReportService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ReceivedClaimTotalReportSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<byte[]> export(PaginationRequest<ReceivedClaimTotalReportSearchDTO> paginationRequest, Locale locale);
}

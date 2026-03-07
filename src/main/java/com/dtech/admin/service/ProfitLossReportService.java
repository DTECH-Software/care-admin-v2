package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ProfitLossReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ProfitLossReportService {

    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ProfitLossReportSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<byte[]> export(PaginationRequest<ProfitLossReportSearchDTO> paginationRequest, Locale locale);
}

package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.LeftEmployeeReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface LeftEmployeeReportService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<LeftEmployeeReportSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<byte[]> export(PaginationRequest<LeftEmployeeReportSearchDTO> paginationRequest, Locale locale);
}

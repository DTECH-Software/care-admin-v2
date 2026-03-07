package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DependentRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.DependentReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface DependentReportService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<DependentReportSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> view(DependentRequestDTO dependentRequestDTO, Locale locale);

    ResponseEntity<byte[]> export(PaginationRequest<DependentReportSearchDTO> paginationRequest, Locale locale);
}

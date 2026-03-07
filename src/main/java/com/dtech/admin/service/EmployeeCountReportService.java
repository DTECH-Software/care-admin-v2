package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeCountReportRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeCountReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface EmployeeCountReportService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeCountReportSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> view(EmployeeCountReportRequestDTO employeeCountReportRequestDTO, Locale locale);

    ResponseEntity<byte[]> export(PaginationRequest<EmployeeCountReportSearchDTO> paginationRequest, Locale locale);
}

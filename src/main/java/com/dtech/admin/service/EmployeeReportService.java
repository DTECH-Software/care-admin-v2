package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface EmployeeReportService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeReportSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> view(EmployeeDetailsRequestDTO employeeDetailsRequestDTO, Locale locale);

    ResponseEntity<byte[]> export(PaginationRequest<EmployeeReportSearchDTO> paginationRequest, Locale locale);
}

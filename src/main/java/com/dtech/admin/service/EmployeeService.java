package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.EmployeeManagementRequestDTO;
import com.dtech.admin.dto.request.EmployeePreviousEmploymentRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface EmployeeService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> add(EmployeeDetailsRequestDTO employeeDetailsRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(EmployeeDetailsRequestDTO employeeDetailsRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> update(EmployeeDetailsRequestDTO employeeDetailsRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> delete(EmployeeDetailsRequestDTO employeeDetailsRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> previousEmployment(EmployeePreviousEmploymentRequestDTO request, Locale locale);
}

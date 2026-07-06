package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.EmployeeManagementRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface EmployeeUserManagementService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> getDependents(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> getLimitDetails(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> update(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> staffCategoryUpdate(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> staffCategoryTransfer(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale);

}

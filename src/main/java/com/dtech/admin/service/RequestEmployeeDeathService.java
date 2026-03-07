package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ClaimRequestDTO;
import com.dtech.admin.dto.request.EmployeeDeathRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface RequestEmployeeDeathService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> request(EmployeeDeathRequestDTO employeeDeathRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ClaimRequestSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(ClaimRequestDTO claimRequestDTO, Locale locale);
}

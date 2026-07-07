package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DependentRequestDTO;
import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ClaimDependentSearchDTO;
import com.dtech.admin.dto.search.EmployeeSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface DependentService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ClaimDependentSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> update(DependentRequestDTO dependentRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> detailsUpdate(DependentRequestDTO dependentRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(DependentRequestDTO dependentRequestDTO, Locale locale);
}

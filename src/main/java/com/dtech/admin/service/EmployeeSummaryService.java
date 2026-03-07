package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeSummaryClaimViewRequestDTO;
import com.dtech.admin.dto.request.EmployeeSummaryRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeSummarySearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface EmployeeSummaryService {

    ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> getEmployeeName(EmployeeSummaryRequestDTO requestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeSummarySearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> getTreatmentBalances(EmployeeSummaryRequestDTO requestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> view(EmployeeSummaryClaimViewRequestDTO requestDTO, Locale locale);
}

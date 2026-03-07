package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.CivilStatusApprovalRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.CivilStatusChangeSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface EmployeeCivilStatusApprovalService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<CivilStatusChangeSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> imageRequest(CivilStatusApprovalRequestDTO civilStatusApprovalRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> updateStatus(CivilStatusApprovalRequestDTO civilStatusApprovalRequestDTO, Locale locale);

}

package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.ThirdPartyIndoorClaimBatchRequestDTO;
import com.dtech.admin.dto.request.ThirdPartyIndoorClaimFileRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ThirdPartyIndoorClaimBatchSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ThirdPartyCriticalClaimImportService {
    ResponseEntity<ApiResponse<Object>> getCriticalReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<byte[]> downloadCriticalTemplate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> validateCritical(ThirdPartyIndoorClaimFileRequestDTO requestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> importCriticalClaims(ThirdPartyIndoorClaimFileRequestDTO requestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterCriticalList(PaginationRequest<ThirdPartyIndoorClaimBatchSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> viewCritical(ThirdPartyIndoorClaimBatchRequestDTO requestDTO, Locale locale);
}

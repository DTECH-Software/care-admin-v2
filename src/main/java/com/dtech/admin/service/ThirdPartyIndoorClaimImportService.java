package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.ThirdPartyIndoorClaimBatchRequestDTO;
import com.dtech.admin.dto.request.ThirdPartyIndoorClaimFileRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ThirdPartyIndoorClaimBatchSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ThirdPartyIndoorClaimImportService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<byte[]> downloadTemplate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> validate(ThirdPartyIndoorClaimFileRequestDTO requestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> importClaims(ThirdPartyIndoorClaimFileRequestDTO requestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ThirdPartyIndoorClaimBatchSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(ThirdPartyIndoorClaimBatchRequestDTO requestDTO, Locale locale);
}


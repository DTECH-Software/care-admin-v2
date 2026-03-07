package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ChequePaymentCreateDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ChequePaymentSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ChequePaymentDdfService {

    ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> create(ChequePaymentCreateDTO createDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ChequePaymentSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> view(ChannelRequestDTO requestDTO, Long id, Locale locale);

    ResponseEntity<byte[]> export(PaginationRequest<ChequePaymentSearchDTO> paginationRequest, Locale locale);
}

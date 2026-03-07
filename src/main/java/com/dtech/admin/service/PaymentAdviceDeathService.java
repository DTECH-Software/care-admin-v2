package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.PaymentAdviceDeathCreateDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.PaymentAdviceDeathClaimSearchDTO;
import com.dtech.admin.dto.search.PaymentAdviceDeathSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface PaymentAdviceDeathService {
    ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterEligibleClaims(
            PaginationRequest<PaymentAdviceDeathClaimSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> create(PaymentAdviceDeathCreateDTO paymentAdviceCreateDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filter(
            PaginationRequest<PaymentAdviceDeathSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> view(ChannelRequestDTO requestDTO, Long id, Locale locale);

    ResponseEntity<String> print(ChannelRequestDTO requestDTO, Long id, Locale locale);

    ResponseEntity<byte[]> printPdf(ChannelRequestDTO requestDTO, Long id, Locale locale);
}

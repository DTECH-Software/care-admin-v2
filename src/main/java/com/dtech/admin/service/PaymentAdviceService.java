package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.PaymentAdviceCreateDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.PaymentAdviceAttachmentSearchDTO;
import com.dtech.admin.dto.search.PaymentAdviceSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface PaymentAdviceService {
    ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterEligibleAttachments(PaginationRequest<PaymentAdviceAttachmentSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> create(PaymentAdviceCreateDTO paymentAdviceCreateDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<PaymentAdviceSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> view(ChannelRequestDTO requestDTO, Long id, Locale locale);

    ResponseEntity<String> print(ChannelRequestDTO requestDTO, Long id, Locale locale);

    ResponseEntity<byte[]> printPdf(ChannelRequestDTO requestDTO, Long id, Locale locale);
}

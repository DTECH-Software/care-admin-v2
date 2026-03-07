package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.PaymentAttachmentActionDTO;
import com.dtech.admin.dto.request.PaymentAttachmentCreateDTO;
import com.dtech.admin.dto.request.PaymentAttachmentStatusUpdateDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.PaymentAttachmentClaimSearchDTO;
import com.dtech.admin.dto.search.PaymentAttachmentSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface PaymentAttachmentService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> getReceivedReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> getSettledReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterEligibleClaims(PaginationRequest<PaymentAttachmentClaimSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> create(PaymentAttachmentCreateDTO paymentAttachmentCreateDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(ChannelRequestDTO requestDTO, Long id, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterAttachments(PaginationRequest<PaymentAttachmentSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterReceivedAttachments(PaginationRequest<PaymentAttachmentSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> updateStatus(PaymentAttachmentStatusUpdateDTO statusUpdateDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> finalizeAttachment(PaymentAttachmentActionDTO actionDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> rejectAttachment(PaymentAttachmentActionDTO actionDTO, Locale locale);
    ResponseEntity<String> print(ChannelRequestDTO requestDTO, Long id, Locale locale);
    ResponseEntity<byte[]> printPdf(ChannelRequestDTO requestDTO, Long id, Locale locale);
    ResponseEntity<byte[]> export(ChannelRequestDTO requestDTO, Long id, Locale locale);
}

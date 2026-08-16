package com.dtech.admin.service;

import com.dtech.admin.dto.request.*;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.SupportTicketSearchDTO;
import com.dtech.admin.enums.SupportTicketSystemType;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface SupportTicketService {
    ResponseEntity<ApiResponse<Object>> referenceData(ChannelRequestDTO request, SupportTicketSystemType systemType, Locale locale);
    ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<SupportTicketSearchDTO> request, SupportTicketSystemType systemType, Locale locale);
    ResponseEntity<ApiResponse<Object>> create(SupportTicketCreateRequestDTO request, SupportTicketSystemType systemType, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(SupportTicketViewRequestDTO request, SupportTicketSystemType systemType, Locale locale);
    ResponseEntity<ApiResponse<Object>> reply(SupportTicketReplyRequestDTO request, SupportTicketSystemType systemType, Locale locale);
    ResponseEntity<ApiResponse<Object>> updateStatus(SupportTicketStatusUpdateRequestDTO request, SupportTicketSystemType systemType, Locale locale);
}

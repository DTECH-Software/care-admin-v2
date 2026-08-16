package com.dtech.admin.controller;

import com.dtech.admin.dto.request.*;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.SupportTicketSearchDTO;
import com.dtech.admin.enums.SupportTicketSystemType;
import com.dtech.admin.service.SupportTicketService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping("api/v1/support-tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupportTicketController {
    private final SupportTicketService supportTicketService;
    private final Gson gson;

    @PostMapping(path = "/wecare-admin/reference-data", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> adminReferenceData(@RequestBody @Valid ChannelRequestValidatorDTO request, Locale locale) {
        return referenceData(request, SupportTicketSystemType.WECARE_ADMIN, locale);
    }

    @PostMapping(path = "/wecare-app/reference-data", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> appReferenceData(@RequestBody @Valid ChannelRequestValidatorDTO request, Locale locale) {
        return referenceData(request, SupportTicketSystemType.WECARE_APP, locale);
    }

    @PostMapping(path = "/wecare-admin/filter", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> adminFilter(@RequestBody @Valid PaginationRequest<SupportTicketSearchDTO> request, Locale locale) {
        return filter(request, SupportTicketSystemType.WECARE_ADMIN, locale);
    }

    @PostMapping(path = "/wecare-app/filter", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> appFilter(@RequestBody @Valid PaginationRequest<SupportTicketSearchDTO> request, Locale locale) {
        return filter(request, SupportTicketSystemType.WECARE_APP, locale);
    }

    @PostMapping(path = "/wecare-admin/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> adminCreate(@RequestBody @Valid SupportTicketCreateRequestDTO request, Locale locale) {
        return supportTicketService.create(request, SupportTicketSystemType.WECARE_ADMIN, locale);
    }

    @PostMapping(path = "/wecare-app/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> appCreate(@RequestBody @Valid SupportTicketCreateRequestDTO request, Locale locale) {
        return supportTicketService.create(request, SupportTicketSystemType.WECARE_APP, locale);
    }

    @PostMapping(path = "/wecare-admin/view", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> adminView(@RequestBody @Valid SupportTicketViewRequestDTO request, Locale locale) {
        return supportTicketService.view(request, SupportTicketSystemType.WECARE_ADMIN, locale);
    }

    @PostMapping(path = "/wecare-app/view", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> appView(@RequestBody @Valid SupportTicketViewRequestDTO request, Locale locale) {
        return supportTicketService.view(request, SupportTicketSystemType.WECARE_APP, locale);
    }

    @PostMapping(path = "/wecare-admin/reply", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> adminReply(@RequestBody @Valid SupportTicketReplyRequestDTO request, Locale locale) {
        return supportTicketService.reply(request, SupportTicketSystemType.WECARE_ADMIN, locale);
    }

    @PostMapping(path = "/wecare-app/reply", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> appReply(@RequestBody @Valid SupportTicketReplyRequestDTO request, Locale locale) {
        return supportTicketService.reply(request, SupportTicketSystemType.WECARE_APP, locale);
    }

    @PostMapping(path = "/wecare-admin/status/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> adminStatusUpdate(@RequestBody @Valid SupportTicketStatusUpdateRequestDTO request, Locale locale) {
        return supportTicketService.updateStatus(request, SupportTicketSystemType.WECARE_ADMIN, locale);
    }

    @PostMapping(path = "/wecare-app/status/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> appStatusUpdate(@RequestBody @Valid SupportTicketStatusUpdateRequestDTO request, Locale locale) {
        return supportTicketService.updateStatus(request, SupportTicketSystemType.WECARE_APP, locale);
    }

    private ResponseEntity<ApiResponse<Object>> referenceData(ChannelRequestValidatorDTO request,
                                                              SupportTicketSystemType systemType,
                                                              Locale locale) {
        return supportTicketService.referenceData(
                gson.fromJson(gson.toJson(request), ChannelRequestDTO.class), systemType, locale);
    }

    private ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<SupportTicketSearchDTO> request,
                                                       SupportTicketSystemType systemType,
                                                       Locale locale) {
        Type type = new TypeToken<PaginationRequest<SupportTicketSearchDTO>>() { }.getType();
        return supportTicketService.filter(gson.fromJson(gson.toJson(request), type), systemType, locale);
    }
}

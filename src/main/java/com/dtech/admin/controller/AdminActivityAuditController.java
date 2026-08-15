package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.AuditLogViewRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.AuditLogSearchDTO;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.service.SourceActivityAuditService;
import com.dtech.admin.validator.OnGet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.Locale;

import static com.dtech.admin.service.impl.SourceActivityAuditServiceImpl.ADMIN_SOURCE;

@RestController
@RequestMapping("api/v1/audit-logs/admin-activity")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminActivityAuditController {
    private static final String PAGE_CODE = WebPage.ADIT_ADMIN.name();
    private final SourceActivityAuditService service;
    private final Gson gson;

    @PostMapping(path = "/reference-data", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> referenceData(
            @RequestBody @Valid ChannelRequestValidatorDTO request, Locale locale) {
        ChannelRequestDTO channelRequest = gson.fromJson(gson.toJson(request), ChannelRequestDTO.class);
        return service.getReferenceData(channelRequest, ADMIN_SOURCE, PAGE_CODE, locale);
    }

    @PostMapping(path = "/filter", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> filter(
            @RequestBody @Valid PaginationRequest<AuditLogSearchDTO> request, Locale locale) {
        Type type = new TypeToken<PaginationRequest<AuditLogSearchDTO>>() { }.getType();
        PaginationRequest<AuditLogSearchDTO> pagination = gson.fromJson(gson.toJson(request), type);
        return service.filter(pagination, ADMIN_SOURCE, PAGE_CODE, locale);
    }

    @PostMapping(path = "/view", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> view(
            @RequestBody @Validated(OnGet.class) @Valid AuditLogViewRequestValidatorDTO request,
            Locale locale) {
        return service.view(request.getId(), request.getUsername(), ADMIN_SOURCE, PAGE_CODE, locale);
    }
}


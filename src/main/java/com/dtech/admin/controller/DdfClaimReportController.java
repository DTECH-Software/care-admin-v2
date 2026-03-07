package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DdfClaimReportRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.DdfClaimReportRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.DdfClaimReportSearchDTO;
import com.dtech.admin.service.DdfClaimReportService;
import com.dtech.admin.validator.OnGet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/reports/ddf-claim-list")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DdfClaimReportController {

    @Autowired
    private final DdfClaimReportService ddfClaimReportService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle DDF claim report reference data request", notes = "DDF claim report reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("DDF claim report reference data request {}", validatorDTO);
        return ddfClaimReportService.getReferenceDate(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle DDF claim report filter request", notes = "DDF claim report filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<DdfClaimReportSearchDTO> paginationRequest, Locale locale) {
        log.info("DDF claim report filter request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<DdfClaimReportSearchDTO>>() {}.getType();
        return ddfClaimReportService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle DDF claim report view request", notes = "DDF claim report view request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid DdfClaimReportRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("DDF claim report view request {}", validatorDTO);
        return ddfClaimReportService.view(gson.fromJson(gson.toJson(validatorDTO), DdfClaimReportRequestDTO.class), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle DDF claim report export request", notes = "DDF claim report export request success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Valid PaginationRequest<DdfClaimReportSearchDTO> paginationRequest, Locale locale) {
        log.info("DDF claim report export request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<DdfClaimReportSearchDTO>>() {}.getType();
        return ddfClaimReportService.export(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }
}

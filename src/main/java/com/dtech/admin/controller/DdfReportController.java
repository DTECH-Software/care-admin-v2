package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DdfReportRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.DdfReportRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.DdfReportSearchDTO;
import com.dtech.admin.service.DdfReportService;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/reports/ddf-report")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DdfReportController {

    @Autowired
    private final DdfReportService ddfReportService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle DDF report reference data request", notes = "DDF report reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("DDF report reference data request {}", validatorDTO);
        return ddfReportService.getReferenceDate(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle DDF report filter request", notes = "DDF report filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<DdfReportSearchDTO> paginationRequest, Locale locale) {
        log.info("DDF report filter request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<DdfReportSearchDTO>>() {}.getType();
        return ddfReportService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle DDF report view request", notes = "DDF report view request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid DdfReportRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("DDF report view request {}", validatorDTO);
        return ddfReportService.view(gson.fromJson(gson.toJson(validatorDTO), DdfReportRequestDTO.class), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle DDF report export request", notes = "DDF report export request success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Valid PaginationRequest<DdfReportSearchDTO> paginationRequest, Locale locale) {
        log.info("DDF report export request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<DdfReportSearchDTO>>() {}.getType();
        return ddfReportService.export(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }
}

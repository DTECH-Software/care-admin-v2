package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DependentRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.DependentRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.DependentReportSearchDTO;
import com.dtech.admin.service.DependentReportService;
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
@RequestMapping(path = "api/v1/reports/dependent-list")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DependentReportController {

    @Autowired
    private final DependentReportService dependentReportService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dependent report reference data request", notes = "Dependent report reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Dependent report reference data request {}", validatorDTO);
        return dependentReportService.getReferenceDate(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dependent report filter request", notes = "Dependent report filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<DependentReportSearchDTO> paginationRequest, Locale locale) {
        log.info("Dependent report filter request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<DependentReportSearchDTO>>() {}.getType();
        return dependentReportService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dependent report view request", notes = "Dependent report view request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid DependentRequestValidatorDTO dependentRequestValidatorDTO, Locale locale) {
        log.info("Dependent report view request {}", dependentRequestValidatorDTO);
        return dependentReportService.view(gson.fromJson(gson.toJson(dependentRequestValidatorDTO), DependentRequestDTO.class), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dependent report export request", notes = "Dependent report export request success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Valid PaginationRequest<DependentReportSearchDTO> paginationRequest, Locale locale) {
        log.info("Dependent report export request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<DependentReportSearchDTO>>() {}.getType();
        return dependentReportService.export(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }
}

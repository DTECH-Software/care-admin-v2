package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.LeftEmployeeReportSearchDTO;
import com.dtech.admin.service.LeftEmployeeReportService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/reports/employee-left")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LeftEmployeeReportController {

    @Autowired
    private final LeftEmployeeReportService leftEmployeeReportService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle left employee report reference data request", notes = "Left employee report reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Left employee report reference data request {}", validatorDTO);
        return leftEmployeeReportService.getReferenceDate(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle left employee report filter request", notes = "Left employee report filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<LeftEmployeeReportSearchDTO> paginationRequest, Locale locale) {
        log.info("Left employee report filter request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<LeftEmployeeReportSearchDTO>>() {}.getType();
        return leftEmployeeReportService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle left employee report export request", notes = "Left employee report export request success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Valid PaginationRequest<LeftEmployeeReportSearchDTO> paginationRequest, Locale locale) {
        log.info("Left employee report export request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<LeftEmployeeReportSearchDTO>>() {}.getType();
        return leftEmployeeReportService.export(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }
}

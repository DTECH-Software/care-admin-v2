package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeCountReportRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeeCountReportRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeCountReportSearchDTO;
import com.dtech.admin.service.EmployeeCountReportService;
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
import com.dtech.admin.validator.OnGet;

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/reports/employee-count")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeCountReportController {

    @Autowired
    private final EmployeeCountReportService employeeCountReportService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee count report reference data request", notes = "Employee count report reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Employee count report reference data request {}", validatorDTO);
        return employeeCountReportService.getReferenceDate(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee count report filter request", notes = "Employee count report filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<EmployeeCountReportSearchDTO> paginationRequest, Locale locale) {
        log.info("Employee count report filter request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<EmployeeCountReportSearchDTO>>() {}.getType();
        return employeeCountReportService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee count report view request", notes = "Employee count report view request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid EmployeeCountReportRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Employee count report view request {}", validatorDTO);
        return employeeCountReportService.view(gson.fromJson(gson.toJson(validatorDTO), EmployeeCountReportRequestDTO.class), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee count report export request", notes = "Employee count report export request success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Valid PaginationRequest<EmployeeCountReportSearchDTO> paginationRequest, Locale locale) {
        log.info("Employee count report export request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<EmployeeCountReportSearchDTO>>() {}.getType();
        return employeeCountReportService.export(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }
}

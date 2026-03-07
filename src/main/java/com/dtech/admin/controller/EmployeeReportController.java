package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeeDetailsRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeReportSearchDTO;
import com.dtech.admin.service.EmployeeReportService;
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
@RequestMapping(path = "api/v1/reports/employee-list")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeReportController {

    @Autowired
    private final EmployeeReportService employeeReportService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee report reference data request", notes = "Employee report reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Employee report reference data request {}", validatorDTO);
        return employeeReportService.getReferenceDate(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee report filter request", notes = "Employee report filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<EmployeeReportSearchDTO> paginationRequest, Locale locale) {
        log.info("Employee report filter request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<EmployeeReportSearchDTO>>() {}.getType();
        return employeeReportService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee report view request", notes = "Employee report view request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid EmployeeDetailsRequestValidatorDTO employeeDetailsRequestValidatorDTO, Locale locale) {
        log.info("Employee report view request {}", employeeDetailsRequestValidatorDTO);
        return employeeReportService.view(gson.fromJson(gson.toJson(employeeDetailsRequestValidatorDTO), EmployeeDetailsRequestDTO.class), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee report export request", notes = "Employee report export request success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Valid PaginationRequest<EmployeeReportSearchDTO> paginationRequest, Locale locale) {
        log.info("Employee report export request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<EmployeeReportSearchDTO>>() {}.getType();
        return employeeReportService.export(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }
}

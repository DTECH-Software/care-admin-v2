package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeSummaryClaimViewRequestDTO;
import com.dtech.admin.dto.request.EmployeeSummaryRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeeSummaryClaimViewValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeeSummaryRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeSummarySearchDTO;
import com.dtech.admin.service.EmployeeSummaryService;
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
@RequestMapping(path = "api/v1/summary/employee")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeSummaryController {

    @Autowired
    private final EmployeeSummaryService employeeSummaryService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee summary reference data request", notes = "Employee summary reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Employee summary reference data request {}", validatorDTO);
        return employeeSummaryService.getReferenceData(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/employee-name", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee summary name request", notes = "Employee summary name request success or failed")
    public ResponseEntity<ApiResponse<Object>> getEmployeeName(@RequestBody @Validated(OnGet.class) @Valid EmployeeSummaryRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Employee summary employee name request {}", validatorDTO);
        return employeeSummaryService.getEmployeeName(gson.fromJson(gson.toJson(validatorDTO), EmployeeSummaryRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee summary filter list request", notes = "Employee summary filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<EmployeeSummarySearchDTO> paginationRequest, Locale locale) {
        log.info("Employee summary filter list request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<EmployeeSummarySearchDTO>>() {}.getType();
        return employeeSummaryService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/balance", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee summary balance request", notes = "Employee summary balance request success or failed")
    public ResponseEntity<ApiResponse<Object>> getBalance(@RequestBody @Validated(OnGet.class) @Valid EmployeeSummaryRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Employee summary balance request {}", validatorDTO);
        return employeeSummaryService.getTreatmentBalances(gson.fromJson(gson.toJson(validatorDTO), EmployeeSummaryRequestDTO.class), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee summary view request", notes = "Employee summary view request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid EmployeeSummaryClaimViewValidatorDTO validatorDTO, Locale locale) {
        log.info("Employee summary view request {}", validatorDTO);
        return employeeSummaryService.view(gson.fromJson(gson.toJson(validatorDTO), EmployeeSummaryClaimViewRequestDTO.class), locale);
    }
}

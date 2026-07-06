package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.EmployeeManagementRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeeDetailsRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeeManagementRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeSearchDTO;
import com.dtech.admin.service.EmployeeUserManagementService;
import com.dtech.admin.validator.OnGet;
import com.dtech.admin.validator.OnStaffCategoryUpdate;
import com.dtech.admin.validator.OnUpdate;
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
@RequestMapping(path = "api/v1/employee-user")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeUserManagementController {

    @Autowired
    private final EmployeeUserManagementService employeeUserManagementService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee user details reference data find request request ",notes = "Employee user details reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Employee user details reference data request reference data controller {} ", channelRequestValidatorDTO);
        return employeeUserManagementService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee user details filter list request request ",notes = "Employee user details filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<EmployeeSearchDTO> paginationRequest, Locale locale) {
        log.info("Employee user details filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<EmployeeSearchDTO>>(){}.getType();
        return employeeUserManagementService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/dependent-view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dependent view details find by ID request request ",notes = "Dependent view details find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> getDependents(@RequestBody @Validated(OnGet.class) @Valid EmployeeManagementRequestValidatorDTO employeeManagementRequestValidatorDTO, Locale locale) {
        log.info("Dependent view find by ID request controller {} ", employeeManagementRequestValidatorDTO);
        return employeeUserManagementService.getDependents(gson.fromJson(gson.toJson(employeeManagementRequestValidatorDTO), EmployeeManagementRequestDTO.class), locale);
    }

    @PostMapping(path = "/limit-view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user limit details find by ID request request ",notes = "User limit details find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> getLimitDetails(@RequestBody @Validated(OnGet.class) @Valid EmployeeManagementRequestValidatorDTO employeeManagementRequestValidatorDTO, Locale locale) {
        log.info("User limit find by ID request controller {} ", employeeManagementRequestValidatorDTO);
        return employeeUserManagementService.getLimitDetails(gson.fromJson(gson.toJson(employeeManagementRequestValidatorDTO), EmployeeManagementRequestDTO.class), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user details find by ID request request ",notes = "User details find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid EmployeeManagementRequestValidatorDTO employeeManagementRequestValidatorDTO, Locale locale) {
        log.info("User  details find by ID request controller {} ", employeeManagementRequestValidatorDTO);
        return employeeUserManagementService.view(gson.fromJson(gson.toJson(employeeManagementRequestValidatorDTO), EmployeeManagementRequestDTO.class), locale);
    }

    @PostMapping(path = "/staff-category-update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee staff category update request request ",notes = "Employee staff category update request success or failed")
    public ResponseEntity<ApiResponse<Object>> staffCategoryUpdate(@RequestBody @Validated(OnStaffCategoryUpdate.class) @Valid EmployeeManagementRequestValidatorDTO employeeManagementRequestValidatorDTO, Locale locale) {
        log.info("Employee staff category update request  controller {} ", employeeManagementRequestValidatorDTO);
        return employeeUserManagementService.staffCategoryUpdate(gson.fromJson(gson.toJson(employeeManagementRequestValidatorDTO), EmployeeManagementRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee staff category update request request ",notes = "Employee staff category update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid EmployeeManagementRequestValidatorDTO employeeManagementRequestValidatorDTO, Locale locale) {
        log.info("Employee user update request controller {} ", employeeManagementRequestValidatorDTO);
        return employeeUserManagementService.update(gson.fromJson(gson.toJson(employeeManagementRequestValidatorDTO), EmployeeManagementRequestDTO.class), locale);
    }

}

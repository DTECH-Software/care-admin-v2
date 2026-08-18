package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.EmployeeManagementRequestDTO;
import com.dtech.admin.dto.request.EmployeePreviousEmploymentRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeeDetailsRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeeManagementRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeePreviousEmploymentRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.EmployeeSearchDTO;
import com.dtech.admin.service.EmployeeService;
import com.dtech.admin.service.EmployeeUserManagementService;
import com.dtech.admin.validator.*;
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
@RequestMapping(path = "api/v1/employee")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private final EmployeeService employeeService;

    @Autowired
    private final EmployeeUserManagementService employeeUserManagementService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee details reference data find request request ",notes = "Employee details reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Employee details reference data request reference data controller {} ", channelRequestValidatorDTO);
        return employeeService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/add",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee details add request request ",notes = "Employee details add request success or failed")
    public ResponseEntity<ApiResponse<Object>> add(@RequestBody @Validated(OnAdd.class) @Valid EmployeeDetailsRequestValidatorDTO employeeDetailsRequestValidatorDTO, Locale locale) {
        log.info("Employee details add request controller {} ", employeeDetailsRequestValidatorDTO);
        return employeeService.add(gson.fromJson(gson.toJson(employeeDetailsRequestValidatorDTO), EmployeeDetailsRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee details filter list request request ",notes = "Employee details filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<EmployeeSearchDTO> paginationRequest, Locale locale) {
        log.info("Employee details filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<EmployeeSearchDTO>>(){}.getType();
        return employeeService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee details find by ID request request ",notes = "Employee details find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid EmployeeDetailsRequestValidatorDTO employeeDetailsRequestValidatorDTO, Locale locale) {
        log.info("Pages find by ID request controller {} ", employeeDetailsRequestValidatorDTO);
        return employeeService.view(gson.fromJson(gson.toJson(employeeDetailsRequestValidatorDTO), EmployeeDetailsRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee details update request request ",notes = "Employee details update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid EmployeeDetailsRequestValidatorDTO employeeDetailsRequestValidatorDTO, Locale locale) {
        log.info("pages update request  controller {} ", employeeDetailsRequestValidatorDTO);
        return employeeService.update(gson.fromJson(gson.toJson(employeeDetailsRequestValidatorDTO), EmployeeDetailsRequestDTO.class), locale);
    }

    @PostMapping(path = "/previous-employment", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Find previous company and EPF details by NIC",
            notes = "Returns inactive employee history matching the supplied NIC")
    public ResponseEntity<ApiResponse<Object>> previousEmployment(
            @RequestBody @Valid EmployeePreviousEmploymentRequestValidatorDTO request, Locale locale) {
        log.info("Employee previous employment lookup request for NIC {}", request.getNic());
        return employeeService.previousEmployment(
                gson.fromJson(gson.toJson(request), EmployeePreviousEmploymentRequestDTO.class), locale);
    }

    @PostMapping(path = "/staff-category-update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee staff category update request request ",notes = "Employee staff category update request success or failed")
    public ResponseEntity<ApiResponse<Object>> staffCategoryUpdate(@RequestBody @Validated(OnStaffCategoryUpdate.class) @Valid EmployeeManagementRequestValidatorDTO employeeManagementRequestValidatorDTO, Locale locale) {
        log.info("Employee staff category update request controller {} ", employeeManagementRequestValidatorDTO);
        return employeeUserManagementService.staffCategoryUpdate(gson.fromJson(gson.toJson(employeeManagementRequestValidatorDTO), EmployeeManagementRequestDTO.class), locale);
    }

    @PostMapping(path = "/staff-category-transfer",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee staff category transfer request request ",notes = "Employee staff category transfer request success or failed")
    public ResponseEntity<ApiResponse<Object>> staffCategoryTransfer(@RequestBody @Validated(OnStaffCategoryUpdate.class) @Valid EmployeeManagementRequestValidatorDTO employeeManagementRequestValidatorDTO, Locale locale) {
        log.info("Employee staff category transfer request controller {} ", employeeManagementRequestValidatorDTO);
        return employeeUserManagementService.staffCategoryTransfer(gson.fromJson(gson.toJson(employeeManagementRequestValidatorDTO), EmployeeManagementRequestDTO.class), locale);
    }

    @PostMapping(path = "/delete",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle employee details delete request request ",notes = "Employee details delete request success or failed")
    public ResponseEntity<ApiResponse<Object>> delete(@RequestBody @Validated(OnDelete.class) @Valid EmployeeDetailsRequestValidatorDTO employeeDetailsRequestValidatorDTO, Locale locale) {
        log.info("Pages delete request  controller {} ", employeeDetailsRequestValidatorDTO);
        return employeeService.delete(gson.fromJson(gson.toJson(employeeDetailsRequestValidatorDTO), EmployeeDetailsRequestDTO.class), locale);
    }

}

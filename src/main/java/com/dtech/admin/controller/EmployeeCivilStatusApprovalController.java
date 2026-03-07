package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.CivilStatusApprovalRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.CivilStatusApprovalRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.CivilStatusChangeSearchDTO;
import com.dtech.admin.service.EmployeeCivilStatusApprovalService;
import com.dtech.admin.validator.OnGet;
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
@RequestMapping(path = "api/v1/civil-status")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeCivilStatusApprovalController {

    @Autowired
    private final EmployeeCivilStatusApprovalService employeeCivilStatusApprovalService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle civil status details reference data find request request ",notes = "Civil status details reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Civil status details reference data request reference data controller {} ", channelRequestValidatorDTO);
        return employeeCivilStatusApprovalService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle civil status details filter list request request ",notes = "Civil status details filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<CivilStatusChangeSearchDTO> paginationRequest, Locale locale) {
        log.info("Civil status details filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<CivilStatusChangeSearchDTO>>(){}.getType();
        return employeeCivilStatusApprovalService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle Civil status details find by ID request request ",notes = "Civil status details find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> imageRequest(@RequestBody @Validated(OnGet.class) @Valid CivilStatusApprovalRequestValidatorDTO civilStatusApprovalRequestValidatorDTO, Locale locale) {
        log.info("Civil status find by ID request controller {} ", civilStatusApprovalRequestValidatorDTO);
        return employeeCivilStatusApprovalService.imageRequest(gson.fromJson(gson.toJson(civilStatusApprovalRequestValidatorDTO), CivilStatusApprovalRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle civil status details update request request ",notes = "Civil status details update request success or failed")
    public ResponseEntity<ApiResponse<Object>> updateStatus(@RequestBody @Validated(OnUpdate.class) @Valid CivilStatusApprovalRequestValidatorDTO civilStatusApprovalRequestValidatorDTO, Locale locale) {
        log.info("Civil status update request  controller {} ", civilStatusApprovalRequestValidatorDTO);
        return employeeCivilStatusApprovalService.updateStatus(gson.fromJson(gson.toJson(civilStatusApprovalRequestValidatorDTO), CivilStatusApprovalRequestDTO.class), locale);
    }
}

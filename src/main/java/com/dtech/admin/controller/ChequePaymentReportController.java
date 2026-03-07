package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ChequePaymentCreateDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChequePaymentCreateValidatorDTO;
import com.dtech.admin.dto.request.validator.ChequePaymentIdValidatorDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ChequePaymentSearchDTO;
import com.dtech.admin.service.ChequePaymentService;
import com.dtech.admin.validator.OnAdd;
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
@RequestMapping(path = "api/v1/healthclaim/cheque")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChequePaymentReportController {

    @Autowired
    private final ChequePaymentService chequePaymentService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle cheque payment reference data request", notes = "Cheque payment reference data success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Cheque payment reference data {}", validatorDTO);
        return chequePaymentService.getReferenceData(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle cheque payment create request", notes = "Cheque payment create success or failed")
    public ResponseEntity<ApiResponse<Object>> create(@RequestBody @Validated(OnAdd.class) @Valid ChequePaymentCreateValidatorDTO validatorDTO, Locale locale) {
        log.info("Cheque payment create {}", validatorDTO);
        return chequePaymentService.create(gson.fromJson(gson.toJson(validatorDTO), ChequePaymentCreateDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle cheque payment filter list request", notes = "Cheque payment filter list success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<ChequePaymentSearchDTO> paginationRequest, Locale locale) {
        log.info("Cheque payment filter list {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<ChequePaymentSearchDTO>>() {}.getType();
        return chequePaymentService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle cheque payment view request", notes = "Cheque payment view success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid ChequePaymentIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Cheque payment view {}", validatorDTO);
        return chequePaymentService.view(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle cheque payment export request", notes = "Cheque payment export success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Valid PaginationRequest<ChequePaymentSearchDTO> paginationRequest, Locale locale) {
        log.info("Cheque payment export {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<ChequePaymentSearchDTO>>() {}.getType();
        return chequePaymentService.export(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }
}

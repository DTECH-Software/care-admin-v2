package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ClaimRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.ClaimRequestRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.service.MedicalClaimsReportService;
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
@RequestMapping(path = "api/v1/reports/medical-claims")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MedicalClaimsReportController {

    @Autowired
    private final MedicalClaimsReportService medicalClaimsReportService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle medical claims report reference data request", notes = "Medical claims report reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO,
                                                                Locale locale) {
        log.info("Medical claims report reference data request {}", channelRequestValidatorDTO);
        return medicalClaimsReportService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle medical claims report filter request", notes = "Medical claims report filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<ClaimRequestSearchDTO> paginationRequest,
                                                          Locale locale) {
        log.info("Medical claims report filter request {}", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<ClaimRequestSearchDTO>>() {}.getType();
        return medicalClaimsReportService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle medical claims report view request", notes = "Medical claims report view request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid ClaimRequestRequestValidatorDTO validatorDTO,
                                                    Locale locale) {
        log.info("Medical claims report view request {}", validatorDTO);
        return medicalClaimsReportService.view(gson.fromJson(gson.toJson(validatorDTO), ClaimRequestDTO.class), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle medical claims report export request", notes = "Medical claims report export request success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Valid PaginationRequest<ClaimRequestSearchDTO> paginationRequest,
                                         Locale locale) {
        log.info("Medical claims report export request {}", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<ClaimRequestSearchDTO>>() {}.getType();
        return medicalClaimsReportService.export(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }
}

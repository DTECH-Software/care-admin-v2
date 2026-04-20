package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.ThirdPartyIndoorClaimBatchRequestDTO;
import com.dtech.admin.dto.request.ThirdPartyIndoorClaimFileRequestDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ThirdPartyIndoorClaimBatchSearchDTO;
import com.dtech.admin.service.ThirdPartyIndoorClaimImportService;
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
@RequestMapping(path = "api/v1/third-party-indoor-claims")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ThirdPartyIndoorClaimImportController {

    @Autowired
    private final ThirdPartyIndoorClaimImportService thirdPartyIndoorClaimImportService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle third party indoor claim import reference data request", notes = "Third party indoor claim import reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO,
                                                                Locale locale) {
        log.info("Third party indoor claim import reference data request {}", validatorDTO);
        return thirdPartyIndoorClaimImportService.getReferenceDate(
                gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle third party indoor claim import template download", notes = "Third party indoor claim import template download success or failed")
    public ResponseEntity<byte[]> downloadTemplate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO,
                                                   Locale locale) {
        log.info("Third party indoor claim import template request {}", validatorDTO);
        return thirdPartyIndoorClaimImportService.downloadTemplate(
                gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/validate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle third party indoor claim import validate request", notes = "Third party indoor claim import validate request success or failed")
    public ResponseEntity<ApiResponse<Object>> validate(@RequestBody @Valid ThirdPartyIndoorClaimFileRequestDTO requestDTO,
                                                        Locale locale) {
        log.info("Third party indoor claim import validate request {}", requestDTO.getFileName());
        return thirdPartyIndoorClaimImportService.validate(requestDTO, locale);
    }

    @PostMapping(path = "/import", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle third party indoor claim import request", notes = "Third party indoor claim import request success or failed")
    public ResponseEntity<ApiResponse<Object>> importClaims(@RequestBody @Valid ThirdPartyIndoorClaimFileRequestDTO requestDTO,
                                                            Locale locale) {
        log.info("Third party indoor claim import request {}", requestDTO.getFileName());
        return thirdPartyIndoorClaimImportService.importClaims(requestDTO, locale);
    }

    @PostMapping(path = "/filter-list", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle third party indoor claim import history filter request", notes = "Third party indoor claim import history filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<ThirdPartyIndoorClaimBatchSearchDTO> paginationRequest,
                                                          Locale locale) {
        log.info("Third party indoor claim import filter request {}", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<ThirdPartyIndoorClaimBatchSearchDTO>>() {
        }.getType();
        return thirdPartyIndoorClaimImportService.filterList(
                gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle third party indoor claim import history view request", notes = "Third party indoor claim import history view request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Valid ThirdPartyIndoorClaimBatchRequestDTO requestDTO,
                                                    Locale locale) {
        log.info("Third party indoor claim import view request {}", requestDTO.getId());
        return thirdPartyIndoorClaimImportService.view(requestDTO, locale);
    }
}


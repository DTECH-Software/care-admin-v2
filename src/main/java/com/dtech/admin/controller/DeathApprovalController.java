package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ClaimRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.ClaimRequestRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.service.DeathApprovalService;
import com.dtech.admin.validator.OnDeath;
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
@RequestMapping(path = "api/v1/death-approval")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeathApprovalController {

    @Autowired
    private final DeathApprovalService deathApprovalService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death claim approval details reference data find request request ",notes = "Death claim approval details reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Claim approval details reference data request reference data controller {} ", channelRequestValidatorDTO);
        return deathApprovalService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death claim approval details filter list request request ",notes = "Death claim approval details filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<ClaimRequestSearchDTO> paginationRequest, Locale locale) {
        log.info("Claim approval details filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<ClaimRequestSearchDTO>>(){}.getType();
        return deathApprovalService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death claim approval details find by ID request request ",notes = "Death claim approval details find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid ClaimRequestRequestValidatorDTO claimRequestRequestValidatorDTO, Locale locale) {
        log.info("Claim approval find by ID request controller {} ", claimRequestRequestValidatorDTO);
        return deathApprovalService.view(gson.fromJson(gson.toJson(claimRequestRequestValidatorDTO), ClaimRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death claim approval details update request request ",notes = "Death claim approval details update request success or failed")
    public ResponseEntity<ApiResponse<Object>> actionRequest(@RequestBody @Validated(OnDeath.class) @Valid ClaimRequestRequestValidatorDTO claimRequestRequestValidatorDTO, Locale locale) {
        log.info("Claim approval update request  controller {} ", claimRequestRequestValidatorDTO);
        return deathApprovalService.actionRequest(gson.fromJson(gson.toJson(claimRequestRequestValidatorDTO), ClaimRequestDTO.class), locale);
    }

}

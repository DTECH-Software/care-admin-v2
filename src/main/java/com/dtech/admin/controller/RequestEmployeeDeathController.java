package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ClaimRequestDTO;
import com.dtech.admin.dto.request.EmployeeDeathRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.ClaimRequestRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.EmployeeDeathRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.service.RequestEmployeeDeathService;
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
@RequestMapping(path = "api/v1/emp-death")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RequestEmployeeDeathController {

    @Autowired
    private final RequestEmployeeDeathService requestEmployeeDeathService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death claim history details reference data find request request ",notes = "Death claim history details reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Death claim history details reference data request reference data controller {} ", channelRequestValidatorDTO);
        return requestEmployeeDeathService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle insurance claim history details filter list request request ",notes = "Death claim history details filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<ClaimRequestSearchDTO> paginationRequest, Locale locale) {
        log.info("Death claim history details filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<ClaimRequestSearchDTO>>(){}.getType();
        return requestEmployeeDeathService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death claim history details find by ID request request ",notes = "Death claim history details find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid ClaimRequestRequestValidatorDTO claimRequestRequestValidatorDTO, Locale locale) {
        log.info("Death claim history find by ID request controller {} ", claimRequestRequestValidatorDTO);
        return requestEmployeeDeathService.view(gson.fromJson(gson.toJson(claimRequestRequestValidatorDTO), ClaimRequestDTO.class), locale);
    }

    @PostMapping(path = "/request",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death claim history details find by ID request request ",notes = "Death claim history details find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> request(@RequestBody  @Valid EmployeeDeathRequestValidatorDTO deathRequestValidatorDTO, Locale locale) {
        log.info("Death claim history find by ID request controller {} ", deathRequestValidatorDTO);
        return requestEmployeeDeathService.request(gson.fromJson(gson.toJson(deathRequestValidatorDTO), EmployeeDeathRequestDTO.class), locale);
    }
}

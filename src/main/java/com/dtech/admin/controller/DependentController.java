package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DependentRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.DependentRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ClaimDependentSearchDTO;
import com.dtech.admin.service.DependentService;
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
@RequestMapping(path = "api/v1/dependent")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DependentController {

    @Autowired
    private final DependentService dependentService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dependent details reference data find request request ",notes = "Dependent details reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Dependent details reference data request reference data controller {} ", channelRequestValidatorDTO);
        return dependentService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dependent details filter list request request ",notes = "Dependent details filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<ClaimDependentSearchDTO> paginationRequest, Locale locale) {
        log.info("Dependent details filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<ClaimDependentSearchDTO>>(){}.getType();
        return dependentService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dependent details find by ID request request ",notes = "Dependent details find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid DependentRequestValidatorDTO dependentRequestValidatorDTO, Locale locale) {
        log.info("Dependent find by ID request controller {} ", dependentRequestValidatorDTO);
        return dependentService.view(gson.fromJson(gson.toJson(dependentRequestValidatorDTO), DependentRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dependent details update request request ",notes = "Dependent details update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid DependentRequestValidatorDTO dependentRequestValidatorDTO, Locale locale) {
        log.info("Dependent update request  controller {} ", dependentRequestValidatorDTO);
        return dependentService.update(gson.fromJson(gson.toJson(dependentRequestValidatorDTO), DependentRequestDTO.class), locale);
    }

}

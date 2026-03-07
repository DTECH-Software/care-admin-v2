/**
 * User: Himal_J
 * Date: 4/30/2025
 * Time: 10:30 AM
 * <p>
 */

package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.SystemUserRequestDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.SystemUserRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.SystemUserSearchDTO;
import com.dtech.admin.service.SystemUserService;
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
@RequestMapping(path = "api/v1/user")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SystemUserController {

    @Autowired
    private final SystemUserService systemUserService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle system user reference data find request request ",notes = "System user reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("System user reference data request reference data controller {} ", channelRequestValidatorDTO);
        return systemUserService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle system user filter list request request ",notes = "System user filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<SystemUserSearchDTO> paginationRequest, Locale locale) {
        log.info("System user filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<SystemUserSearchDTO>>(){}.getType();
        return systemUserService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/add",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle system user add request request ",notes = "System user add request success or failed")
    public ResponseEntity<ApiResponse<Object>> add(@RequestBody @Validated(OnAdd.class) @Valid SystemUserRequestValidatorDTO systemUserRequestValidatorDTO, Locale locale) {
        log.info("System user add request controller {} ", systemUserRequestValidatorDTO);
        return systemUserService.add(gson.fromJson(gson.toJson(systemUserRequestValidatorDTO), SystemUserRequestDTO.class), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle system user find by ID request request ",notes = "System user find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid SystemUserRequestValidatorDTO systemUserRequestValidatorDTO, Locale locale) {
        log.info("System user find by ID request controller {} ", systemUserRequestValidatorDTO);
        return systemUserService.view(gson.fromJson(gson.toJson(systemUserRequestValidatorDTO), SystemUserRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle system user update request request ",notes = "System user update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid SystemUserRequestValidatorDTO systemUserRequestValidatorDTO, Locale locale) {
        log.info("System user update request controller {} ", systemUserRequestValidatorDTO);
        return systemUserService.update(gson.fromJson(gson.toJson(systemUserRequestValidatorDTO), SystemUserRequestDTO.class), locale);
    }

    @PostMapping(path = "/delete",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user delete request request ",notes = "User delete request success or failed")
    public ResponseEntity<ApiResponse<Object>> delete(@RequestBody @Validated(OnDelete.class) @Valid SystemUserRequestValidatorDTO systemUserRequestValidatorDTO, Locale locale) {
        log.info("User delete request controller {} ", systemUserRequestValidatorDTO);
        return systemUserService.delete(gson.fromJson(gson.toJson(systemUserRequestValidatorDTO), SystemUserRequestDTO.class), locale);
    }

    @PostMapping(path = "/sent-otp",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user sent OTP request request ",notes = "User sent OTP request success or failed")
    public ResponseEntity<ApiResponse<Object>> sentOTP(@RequestBody @Validated(OnOTP.class) @Valid SystemUserRequestValidatorDTO systemUserRequestValidatorDTO, Locale locale) {
        log.info("User sent OTP request controller {} ", systemUserRequestValidatorDTO);
        return systemUserService.sentOTP(gson.fromJson(gson.toJson(systemUserRequestValidatorDTO), SystemUserRequestDTO.class), locale);
    }
}

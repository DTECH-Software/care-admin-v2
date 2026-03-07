/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 12:28 PM
 * <p>
 */

package com.dtech.admin.controller;

import com.dtech.admin.dto.search.CommonSearchDTO;
import com.dtech.admin.dto.request.*;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.UserRoleRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.service.UserRoleService;
import com.dtech.admin.validator.OnAdd;
import com.dtech.admin.validator.OnDelete;
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
@RequestMapping(path = "api/v1/user-role")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserRoleController {

    @Autowired
    private final UserRoleService userRoleService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user role reference data find request request ",notes = "User role reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("User role reference data request reference data controller {} ", channelRequestValidatorDTO);
        return userRoleService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user role filter list request request ",notes = "User role filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale) {
        log.info("User role filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<CommonSearchDTO>>(){}.getType();
        return userRoleService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/add",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user role add request request ",notes = "User role add request success or failed")
    public ResponseEntity<ApiResponse<Object>> add(@RequestBody @Validated(OnAdd.class) @Valid UserRoleRequestValidatorDTO userRoleRequestValidatorDTO, Locale locale) {
        log.info("User role add request controller {} ", userRoleRequestValidatorDTO);
        return userRoleService.add(gson.fromJson(gson.toJson(userRoleRequestValidatorDTO), UserRoleRequestDTO.class), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user role find by ID request request ",notes = "User role find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid UserRoleRequestValidatorDTO userRoleRequestValidatorDTO, Locale locale) {
        log.info("User role find by ID request controller {} ", userRoleRequestValidatorDTO);
        return userRoleService.view(gson.fromJson(gson.toJson(userRoleRequestValidatorDTO), UserRoleRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user role update request request ",notes = "User role update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid UserRoleRequestValidatorDTO userRoleRequestValidatorDTO, Locale locale) {
        log.info("User role update request controller {} ", userRoleRequestValidatorDTO);
        return userRoleService.update(gson.fromJson(gson.toJson(userRoleRequestValidatorDTO), UserRoleRequestDTO.class), locale);
    }

    @PostMapping(path = "/delete",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user role delete request request ",notes = "User role delete request success or failed")
    public ResponseEntity<ApiResponse<Object>> delete(@RequestBody @Validated(OnDelete.class) @Valid  UserRoleRequestValidatorDTO userRoleRequestValidatorDTO, Locale locale) {
        log.info("User role delete request  controller {} ", userRoleRequestValidatorDTO);
        return userRoleService.delete(gson.fromJson(gson.toJson(userRoleRequestValidatorDTO), UserRoleRequestDTO.class), locale);
    }

}

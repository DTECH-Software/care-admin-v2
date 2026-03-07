/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 11:49 AM
 * <p>
 */

package com.dtech.admin.controller;

import com.dtech.admin.dto.request.AssignedUnAssignedTaskRequestDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.validator.AssignedUnAssignedTaskRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.service.UserPrivilegeService;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/privilege")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserPrivilegeController {

    @Autowired
    private final UserPrivilegeService userPrivilegeService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle privilege reference data find request request ",notes = "Privilege reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Privilege reference data request reference data controller {} ", channelRequestValidatorDTO);
        return userPrivilegeService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/assigned-unassigned-task",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle assigned un-assigned task find request request ",notes = "Assigned un-assigned task request success or failed")
    public ResponseEntity<ApiResponse<Object>> getTaskByPageAndUserRole(@RequestBody @Valid AssignedUnAssignedTaskRequestValidatorDTO assignedUnAssignedTaskRequestValidatorDTO, Locale locale) {
        log.info("Assigned un-assigned task request reference data controller {} ", assignedUnAssignedTaskRequestValidatorDTO);
        return userPrivilegeService.getTaskByPageAndUserRole(gson.fromJson(gson.toJson(assignedUnAssignedTaskRequestValidatorDTO), AssignedUnAssignedTaskRequestDTO.class), locale);
    }

    @PostMapping(path = "/assigned-privilege",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle update assigned un-assigned task find request request ",notes = "Update assigned un-assigned task request success or failed")
    public ResponseEntity<ApiResponse<Object>> assignedPrivileges(@RequestBody @Valid AssignedUnAssignedTaskRequestValidatorDTO assignedUnAssignedTaskRequestValidatorDTO, Locale locale) {
        log.info("Update assigned un-assigned task request reference data controller {} ", assignedUnAssignedTaskRequestValidatorDTO);
        return userPrivilegeService.assignedPrivileges(gson.fromJson(gson.toJson(assignedUnAssignedTaskRequestValidatorDTO), AssignedUnAssignedTaskRequestDTO.class), locale);
    }

}

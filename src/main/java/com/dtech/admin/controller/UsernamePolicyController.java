/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 12:32 PM
 * <p>
 */

package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.UsernamePolicyRequestDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.UsernamePolicyRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.service.UsernamePolicyService;
import com.dtech.admin.validator.OnUpdate;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/web-username-policy")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UsernamePolicyController {

    @Autowired
    private final UsernamePolicyService usernamePolicyService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle web username policy reference data find request request ",notes = "Web username policy reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Web username policy  reference data request reference data controller {} ", channelRequestValidatorDTO);
        return usernamePolicyService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle web username policy find by ID request request ",notes = "Web username policy find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody  @Valid UsernamePolicyRequestValidatorDTO usernamePolicyRequestValidatorDTO, Locale locale) {
        log.info("Web username policy  find by ID request controller {} ", usernamePolicyRequestValidatorDTO);
        return usernamePolicyService.view(gson.fromJson(gson.toJson(usernamePolicyRequestValidatorDTO), UsernamePolicyRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle web username policy update request request ",notes = "Web username policy update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid UsernamePolicyRequestValidatorDTO usernamePolicyRequestValidatorDTO, Locale locale) {
        log.info("Web username policy update request controller {} ", usernamePolicyRequestValidatorDTO);
        return usernamePolicyService.update(gson.fromJson(gson.toJson(usernamePolicyRequestValidatorDTO), UsernamePolicyRequestDTO.class), locale);
    }
}

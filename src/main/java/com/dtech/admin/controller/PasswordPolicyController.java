/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 10:06 AM
 * <p>
 */

package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PasswordPolicyRequestDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.PasswordPolicyRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.service.PasswordPolicyService;
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
@RequestMapping(path = "api/v1/web-password-policy")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PasswordPolicyController {

    @Autowired
    private final PasswordPolicyService passwordPolicyService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle web password policy reference data find request request ",notes = "Web password policy reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Web password policy  reference data request reference data controller {} ", channelRequestValidatorDTO);
        return passwordPolicyService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle web password policy find by ID request request ",notes = "Web password policy find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Valid PasswordPolicyRequestValidatorDTO passwordPolicyRequestValidatorDTO, Locale locale) {
        log.info("Web password policy  find by ID request controller {} ", passwordPolicyRequestValidatorDTO);
        return passwordPolicyService.view(gson.fromJson(gson.toJson(passwordPolicyRequestValidatorDTO), PasswordPolicyRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle web password policy update request request ",notes = "Web password policy update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid PasswordPolicyRequestValidatorDTO passwordPolicyRequestValidatorDTO, Locale locale) {
        log.info("Web password policy update request controller {} ", passwordPolicyRequestValidatorDTO);
        return passwordPolicyService.update(gson.fromJson(gson.toJson(passwordPolicyRequestValidatorDTO), PasswordPolicyRequestDTO.class), locale);
    }

}

/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 10:55 AM
 * <p>
 */

package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.LoginRequestDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.LoginRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.service.LoginService;
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
@RequestMapping(path = "api/v1/login")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private final LoginService loginService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/login",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle login request request request ",notes = "Login request request success or failed")
    public ResponseEntity<ApiResponse<Object>> login(@RequestBody @Valid LoginRequestValidatorDTO loginRequestValidatorDTO, Locale locale) {
        log.info("Login request reference data controller {} ", loginRequestValidatorDTO);
        return loginService.login(gson.fromJson(gson.toJson(loginRequestValidatorDTO), LoginRequestDTO.class), locale);
    }

    @PostMapping(path = "/left-menu",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle authorizer pages request request ",notes = "Authorizer pages request success or failed")
    public ResponseEntity<ApiResponse<Object>> leftMenu(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Authorizer pages request controller {} ", channelRequestValidatorDTO);
        return loginService.leftMenu(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/logout",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle logout request request request ",notes = "Logout request request success or failed")
    public ResponseEntity<ApiResponse<Object>> logout(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Logout request  controller {} ", channelRequestValidatorDTO);
        return loginService.logout(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

}

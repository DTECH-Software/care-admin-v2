/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 10:55 AM
 * <p>
 */

package com.dtech.admin.controller;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ResetPasswordDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.ResetPasswordValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.service.ResetPasswordService;
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
@RequestMapping(path = "api/v1/password")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResetPasswordController {

    @Autowired
    private final ResetPasswordService resetPasswordService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reset",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle password request request ",notes = "Password reset request success or failed")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody @Valid ResetPasswordValidatorDTO resetPasswordValidatorDTO, Locale locale) {
        log.info("Password reset request controller {} ", resetPasswordValidatorDTO);
        return resetPasswordService.resetPassword(gson.fromJson(gson.toJson(resetPasswordValidatorDTO), ResetPasswordDTO.class), locale);
    }

    @PostMapping(path = "/sent-otp", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle user sent OTP request request ", notes = "User sent OTP request success or failed")
    public ResponseEntity<ApiResponse<Object>> sentOTP(@RequestBody  @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("User sent OTP request controller {} ", channelRequestValidatorDTO);
        return resetPasswordService.requestOTP(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

}

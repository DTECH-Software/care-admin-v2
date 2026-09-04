package com.dtech.admin.controller;

import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CareAdminSplashRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsPublicSplashPayloadWithoutUsername() {
        ChannelRequestValidatorDTO request = new ChannelRequestValidatorDTO();
        request.setIp("0.0.0.1");
        request.setMessage("SPLASH");
        request.setUserAgent("Care Admin Web");

        assertTrue(validator.validate(request).isEmpty());
    }
}

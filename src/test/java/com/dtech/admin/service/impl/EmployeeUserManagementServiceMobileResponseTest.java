package com.dtech.admin.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeUserManagementServiceMobileResponseTest {

    @Test
    void shouldRecognizeSystemGeneratedMobileNumber() {
        assertTrue(EmployeeUserManagementServiceImpl.isDummyMobileNumber("0000000031"));
    }

    @Test
    void shouldNotRecognizeNormalMobileNumberAsSystemGenerated() {
        assertFalse(EmployeeUserManagementServiceImpl.isDummyMobileNumber("0776037678"));
        assertFalse(EmployeeUserManagementServiceImpl.isDummyMobileNumber(null));
    }
}

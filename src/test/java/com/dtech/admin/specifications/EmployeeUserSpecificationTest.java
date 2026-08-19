package com.dtech.admin.specifications;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmployeeUserSpecificationTest {

    @Test
    void shouldCreateEquivalentSearchTermsForLocalMobileNumber() {
        assertEquals(
                Set.of("0771234567", "771234567", "94771234567"),
                EmployeeUserSpecification.mobileSearchTerms("077 123-4567")
        );
    }

    @Test
    void shouldCreateEquivalentSearchTermsForInternationalMobileNumber() {
        assertEquals(
                Set.of("94771234567", "771234567", "0771234567"),
                EmployeeUserSpecification.mobileSearchTerms("+94 (77) 123 4567")
        );
    }
}

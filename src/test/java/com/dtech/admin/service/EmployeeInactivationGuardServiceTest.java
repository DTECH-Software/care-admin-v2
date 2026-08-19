package com.dtech.admin.service;

import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.repository.DeathClaimRequestRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class EmployeeInactivationGuardServiceTest {

    private InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;
    private DeathClaimRequestRepository deathClaimRequestRepository;
    private EmployeeInactivationGuardService service;

    @BeforeEach
    void setUp() {
        insuranceClaimsRequestRepository = mock(InsuranceClaimsRequestRepository.class);
        deathClaimRequestRepository = mock(DeathClaimRequestRepository.class);
        service = new EmployeeInactivationGuardService(
                insuranceClaimsRequestRepository, deathClaimRequestRepository);
    }

    @Test
    void insuranceClaimUnderReviewBlocksInactivation() {
        ApplicationUser employee = new ApplicationUser();
        when(insuranceClaimsRequestRepository.existsByEmployeeAndRequestStatus(
                employee, Workflow.UNDER_REVIEW)).thenReturn(true);

        assertTrue(service.hasUnderReviewClaims(employee));
        verifyNoInteractions(deathClaimRequestRepository);
    }

    @Test
    void deathOrDdfClaimUnderReviewBlocksInactivation() {
        ApplicationUser employee = new ApplicationUser();
        when(deathClaimRequestRepository.existsByEmployeeAndRequestStatus(
                employee, Workflow.UNDER_REVIEW)).thenReturn(true);

        assertTrue(service.hasUnderReviewClaims(employee));
    }

    @Test
    void employeeWithoutUnderReviewClaimsCanBeInactivated() {
        ApplicationUser employee = new ApplicationUser();

        assertFalse(service.hasUnderReviewClaims(employee));
    }
}

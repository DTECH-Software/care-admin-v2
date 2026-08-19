package com.dtech.admin.service;

import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.repository.DeathClaimRequestRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeInactivationGuardService {

    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    public boolean hasUnderReviewClaims(ApplicationUser applicationUser) {
        if (applicationUser == null) {
            return false;
        }
        return insuranceClaimsRequestRepository.existsByEmployeeAndRequestStatus(
                applicationUser, Workflow.UNDER_REVIEW)
                || deathClaimRequestRepository.existsByEmployeeAndRequestStatus(
                applicationUser, Workflow.UNDER_REVIEW);
    }
}

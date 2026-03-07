package com.dtech.admin.service.impl;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.DashboardSummaryResponseDTO;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.repository.ApplicationUserRepository;
import com.dtech.admin.repository.ClaimDependentsRepository;
import com.dtech.admin.repository.DeathClaimRequestRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.service.DashboardService;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Log4j2
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> getSummary(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Dashboard summary request {}", channelRequestDTO.getUsername());

            DashboardSummaryResponseDTO response = new DashboardSummaryResponseDTO();

            DashboardSummaryResponseDTO.EmployeeSummary employeeSummary = new DashboardSummaryResponseDTO.EmployeeSummary();
            employeeSummary.setTotalEmployees(applicationUserRepository.count());
            employeeSummary.setDependentsTotal(claimDependentsRepository.count());
            employeeSummary.setApprovedDependents(claimDependentsRepository.countByStatus(Workflow.APPROVED));
            employeeSummary.setRejectedDependents(claimDependentsRepository.countByStatus(Workflow.REJECTED));
            employeeSummary.setPendingDependents(claimDependentsRepository.countByStatus(Workflow.UNDER_REVIEW));

            DashboardSummaryResponseDTO.ClaimSummary healthSummary = new DashboardSummaryResponseDTO.ClaimSummary();
            healthSummary.setTotal(insuranceClaimsRequestRepository.count());
            healthSummary.setApproved(insuranceClaimsRequestRepository.countByRequestStatus(Workflow.APPROVED));
            healthSummary.setRejected(insuranceClaimsRequestRepository.countByRequestStatus(Workflow.REJECTED));
            healthSummary.setUnderReview(insuranceClaimsRequestRepository.countByRequestStatus(Workflow.UNDER_REVIEW));

            DashboardSummaryResponseDTO.ClaimSummary deathSummary = new DashboardSummaryResponseDTO.ClaimSummary();
            deathSummary.setTotal(deathClaimRequestRepository.count());
            deathSummary.setApproved(deathClaimRequestRepository.countByRequestStatus(Workflow.APPROVED));
            deathSummary.setRejected(deathClaimRequestRepository.countByRequestStatus(Workflow.REJECTED));
            deathSummary.setUnderReview(deathClaimRequestRepository.countByRequestStatus(Workflow.UNDER_REVIEW));

            response.setEmployee(employeeSummary);
            response.setHealthClaims(healthSummary);
            response.setDeathClaims(deathSummary);

            return ResponseEntity.ok().body(
                    responseUtil.success(response,
                            messageSource.getMessage(ResponseMessageUtil.DASHBOARD_SUMMARY_RETRIEVED_SUCCESS, null, locale))
            );
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}

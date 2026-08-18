package com.dtech.admin.service.impl;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.DashboardSummaryResponseDTO;
import com.dtech.admin.enums.Gender;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsurancePolicy;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.repository.ApplicationUserRepository;
import com.dtech.admin.repository.ClaimDependentsRepository;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.DeathClaimRequestRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.repository.InsurancePolicyRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
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
import java.util.Comparator;

@Service
@Log4j2
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final InsurancePolicyRepository insurancePolicyRepository;

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
            employeeSummary.setTotalMaleEmployees(applicationUserRepository.countByUserPersonalDetails_Gender(Gender.MALE));
            employeeSummary.setTotalFemaleEmployees(applicationUserRepository.countByUserPersonalDetails_Gender(Gender.FEMALE));
            employeeSummary.setDependentsTotal(claimDependentsRepository.count());
            employeeSummary.setTotalMaleDependents(claimDependentsRepository.countByGender(Gender.MALE));
            employeeSummary.setTotalFemaleDependents(claimDependentsRepository.countByGender(Gender.FEMALE));
            employeeSummary.setApprovedDependents(claimDependentsRepository.countByStatus(Workflow.APPROVED));
            employeeSummary.setRejectedDependents(claimDependentsRepository.countByStatus(Workflow.REJECTED));
            employeeSummary.setPendingDependents(claimDependentsRepository.countByStatus(Workflow.UNDER_REVIEW));
            response.setCompanies(companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                    .sorted(Comparator.comparing(CompanyTypes::getCode, String.CASE_INSENSITIVE_ORDER))
                    .map(company -> {
                        DashboardSummaryResponseDTO.CompanySummary summary = new DashboardSummaryResponseDTO.CompanySummary();
                        summary.setCode(company.getCode());
                        summary.setDescription(company.getDescription());
                        summary.setTotalEmployees(applicationUserRepository
                                .countByUserPersonalDetails_UserCompanyDetails_CompanyTypes_Code(company.getCode()));
                        return summary;
                    }).toList());
            response.setStaffCategories(staffCategoriesRepository.findAllByStatus(Status.ACTIVE).stream()
                    .sorted(Comparator.comparing(StaffCategories::getCode, String.CASE_INSENSITIVE_ORDER))
                    .map(staffCategory -> {
                        DashboardSummaryResponseDTO.StaffCategorySummary summary =
                                new DashboardSummaryResponseDTO.StaffCategorySummary();
                        summary.setCode(staffCategory.getCode());
                        summary.setDescription(staffCategory.getDescription());
                        summary.setTotalEmployees(applicationUserRepository
                                .countByUserPersonalDetails_UserCompanyDetails_StaffCategories_Code(
                                        staffCategory.getCode()));
                        return summary;
                    }).toList());
            response.setPolicies(insurancePolicyRepository.findAllByStatus(Status.ACTIVE).stream()
                    .sorted(Comparator.comparing(InsurancePolicy::getCode, String.CASE_INSENSITIVE_ORDER))
                    .map(policy -> {
                        DashboardSummaryResponseDTO.PolicySummary summary =
                                new DashboardSummaryResponseDTO.PolicySummary();
                        summary.setCode(policy.getCode());
                        summary.setDescription(policy.getDescription());
                        return summary;
                    }).toList());

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

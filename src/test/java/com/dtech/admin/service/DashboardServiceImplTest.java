package com.dtech.admin.service;

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
import com.dtech.admin.service.impl.DashboardServiceImpl;
import com.dtech.admin.util.ResponseUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {
    @Mock private ApplicationUserRepository applicationUserRepository;
    @Mock private ClaimDependentsRepository claimDependentsRepository;
    @Mock private CompanyTypeRepository companyTypeRepository;
    @Mock private StaffCategoriesRepository staffCategoriesRepository;
    @Mock private InsurancePolicyRepository insurancePolicyRepository;
    @Mock private InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;
    @Mock private DeathClaimRequestRepository deathClaimRequestRepository;
    @Mock private MessageSource messageSource;

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(applicationUserRepository, claimDependentsRepository, companyTypeRepository,
                staffCategoriesRepository, insurancePolicyRepository,
                insuranceClaimsRequestRepository, deathClaimRequestRepository,
                messageSource, new ResponseUtil());
    }

    @Test
    void summaryIncludesMaleAndFemaleEmployeeTotals() {
        when(applicationUserRepository.count()).thenReturn(100L);
        when(applicationUserRepository.countByUserPersonalDetails_Gender(Gender.MALE)).thenReturn(62L);
        when(applicationUserRepository.countByUserPersonalDetails_Gender(Gender.FEMALE)).thenReturn(38L);
        when(claimDependentsRepository.count()).thenReturn(20L);
        when(claimDependentsRepository.countByGender(Gender.MALE)).thenReturn(8L);
        when(claimDependentsRepository.countByGender(Gender.FEMALE)).thenReturn(12L);
        when(claimDependentsRepository.countByStatus(Workflow.APPROVED)).thenReturn(10L);
        when(claimDependentsRepository.countByStatus(Workflow.REJECTED)).thenReturn(4L);
        when(claimDependentsRepository.countByStatus(Workflow.UNDER_REVIEW)).thenReturn(6L);
        CompanyTypes sgcs = company("SGCS", "Samson Group Corporate Services");
        CompanyTypes dsi = company("DSI", "D. Samson Industries");
        when(companyTypeRepository.findAllByStatus(Status.ACTIVE)).thenReturn(List.of(sgcs, dsi));
        when(applicationUserRepository.countByUserPersonalDetails_UserCompanyDetails_CompanyTypes_Code("SGCS"))
                .thenReturn(60L);
        when(applicationUserRepository.countByUserPersonalDetails_UserCompanyDetails_CompanyTypes_Code("DSI"))
                .thenReturn(40L);
        StaffCategories exop1 = staffCategory("EXOP1", "Executive Staff - Option 01");
        StaffCategories exop2 = staffCategory("EXOP2", "Executive Staff - Option 02");
        when(staffCategoriesRepository.findAllByStatus(Status.ACTIVE)).thenReturn(List.of(exop2, exop1));
        when(applicationUserRepository.countByUserPersonalDetails_UserCompanyDetails_StaffCategories_Code("EXOP1"))
                .thenReturn(35L);
        when(applicationUserRepository.countByUserPersonalDetails_UserCompanyDetails_StaffCategories_Code("EXOP2"))
                .thenReturn(65L);
        InsurancePolicy exop2Policy = policy("POL-EXOP2", "Executive Option 02 Policy");
        InsurancePolicy exop1Policy = policy("POL-EXOP1", "Executive Option 01 Policy");
        when(insurancePolicyRepository.findAllByStatus(Status.ACTIVE))
                .thenReturn(List.of(exop2Policy, exop1Policy));
        when(messageSource.getMessage("val.dashboard.summary.retrieved.success", null, Locale.ENGLISH))
                .thenReturn("Dashboard summary retrieved successfully");

        ChannelRequestDTO request = new ChannelRequestDTO();
        request.setUsername("admin.user");
        ResponseEntity<ApiResponse<Object>> result = service.getSummary(request, Locale.ENGLISH);

        assertTrue(result.getBody().isSuccess());
        DashboardSummaryResponseDTO data = (DashboardSummaryResponseDTO) result.getBody().getData();
        assertEquals(100L, data.getEmployee().getTotalEmployees());
        assertEquals(62L, data.getEmployee().getTotalMaleEmployees());
        assertEquals(38L, data.getEmployee().getTotalFemaleEmployees());
        assertEquals(8L, data.getEmployee().getTotalMaleDependents());
        assertEquals(12L, data.getEmployee().getTotalFemaleDependents());
        assertEquals(List.of("DSI", "SGCS"), data.getCompanies().stream()
                .map(DashboardSummaryResponseDTO.CompanySummary::getCode).toList());
        assertEquals(40L, data.getCompanies().get(0).getTotalEmployees());
        assertEquals("Samson Group Corporate Services", data.getCompanies().get(1).getDescription());
        assertEquals(List.of("EXOP1", "EXOP2"), data.getStaffCategories().stream()
                .map(DashboardSummaryResponseDTO.StaffCategorySummary::getCode).toList());
        assertEquals(35L, data.getStaffCategories().get(0).getTotalEmployees());
        assertEquals("Executive Staff - Option 02", data.getStaffCategories().get(1).getDescription());
        assertEquals(List.of("POL-EXOP1", "POL-EXOP2"), data.getPolicies().stream()
                .map(DashboardSummaryResponseDTO.PolicySummary::getCode).toList());
        assertEquals("Executive Option 01 Policy", data.getPolicies().get(0).getDescription());
    }

    private CompanyTypes company(String code, String description) {
        CompanyTypes company = new CompanyTypes();
        company.setCode(code);
        company.setDescription(description);
        company.setStatus(Status.ACTIVE);
        return company;
    }

    private StaffCategories staffCategory(String code, String description) {
        StaffCategories staffCategory = new StaffCategories();
        staffCategory.setCode(code);
        staffCategory.setDescription(description);
        staffCategory.setStatus(Status.ACTIVE);
        return staffCategory;
    }

    private InsurancePolicy policy(String code, String description) {
        InsurancePolicy policy = new InsurancePolicy();
        policy.setCode(code);
        policy.setDescription(description);
        policy.setStatus(Status.ACTIVE);
        return policy;
    }
}

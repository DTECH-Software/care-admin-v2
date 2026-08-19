package com.dtech.admin.service;

import com.dtech.admin.dto.response.InsurancePolicyStaffCategoryGroupResponseDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.InsurancePolicy;
import com.dtech.admin.model.InsurancePolicyStaffCategoryGroup;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.repository.InsurancePolicyStaffCategoryGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsurancePolicyStaffCategoryReferenceServiceTest {

    private InsurancePolicyStaffCategoryGroupRepository repository;
    private InsurancePolicyStaffCategoryReferenceService service;

    @BeforeEach
    void setUp() {
        repository = mock(InsurancePolicyStaffCategoryGroupRepository.class);
        service = new InsurancePolicyStaffCategoryReferenceService(repository);
    }

    @Test
    void returnsActiveStaffCategoriesGroupedByPolicyWithoutDuplicatesAcrossPolicies() {
        InsurancePolicy policyTwo = policy(2L, "POL-02", "Policy 02", Status.ACTIVE);
        InsurancePolicy policyOne = policy(1L, "POL-01", "Policy 01", Status.ACTIVE);
        StaffCategories exop2 = staffCategory("EXOP2", "Executive Option 02", Status.ACTIVE);
        StaffCategories exop1 = staffCategory("EXOP1", "Executive Option 01", Status.ACTIVE);
        StaffCategories inactive = staffCategory("NS", "Non Staff", Status.INACTIVE);

        when(repository.findAllByStatus(Status.ACTIVE)).thenReturn(List.of(
                group(policyTwo, exop2, "EXEC", "Executive"),
                group(policyOne, exop2, "EXEC", "Executive"),
                group(policyOne, exop1, "EXEC", "Executive"),
                group(policyOne, inactive, "NON_STAFF", "Non Staff")));

        List<InsurancePolicyStaffCategoryGroupResponseDTO> result = service.loadActiveGroups();

        assertEquals(2, result.size());
        assertEquals("POL-01", result.get(0).getInsurancePolicy().getCode());
        assertEquals(List.of("EXOP1", "EXOP2"), result.get(0).getStaffCategories().stream()
                .map(InsurancePolicyStaffCategoryGroupResponseDTO.StaffCategoryMapping::getCode)
                .toList());
        assertEquals("EXEC", result.get(0).getStaffCategories().get(0).getMainCategoryCode());
        assertEquals("POL-02", result.get(1).getInsurancePolicy().getCode());
        assertEquals(1, result.get(1).getStaffCategories().size());
    }

    private InsurancePolicy policy(Long id, String code, String description, Status status) {
        InsurancePolicy policy = new InsurancePolicy();
        policy.setId(id);
        policy.setCode(code);
        policy.setDescription(description);
        policy.setStatus(status);
        return policy;
    }

    private StaffCategories staffCategory(String code, String description, Status status) {
        StaffCategories category = new StaffCategories();
        category.setCode(code);
        category.setDescription(description);
        category.setStatus(status);
        return category;
    }

    private InsurancePolicyStaffCategoryGroup group(InsurancePolicy policy,
                                                     StaffCategories staffCategory,
                                                     String mainCode,
                                                     String mainDescription) {
        InsurancePolicyStaffCategoryGroup group = new InsurancePolicyStaffCategoryGroup();
        group.setStatus(Status.ACTIVE);
        group.setInsurancePolicy(policy);
        group.setStaffCategories(staffCategory);
        group.setMainCategoryCode(mainCode);
        group.setMainCategoryDescription(mainDescription);
        return group;
    }
}

package com.dtech.admin.service;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.response.InsurancePolicyStaffCategoryGroupResponseDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.InsurancePolicy;
import com.dtech.admin.model.InsurancePolicyStaffCategoryGroup;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.repository.InsurancePolicyStaffCategoryGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InsurancePolicyStaffCategoryReferenceService {

    private final InsurancePolicyStaffCategoryGroupRepository repository;

    public List<InsurancePolicyStaffCategoryGroupResponseDTO> loadActiveGroups() {
        Map<String, InsurancePolicyStaffCategoryGroupResponseDTO> groupsByPolicy = new LinkedHashMap<>();

        repository.findAllByStatus(Status.ACTIVE).stream()
                .filter(this::hasActiveReferences)
                .sorted(Comparator
                        .comparing((InsurancePolicyStaffCategoryGroup group) ->
                                group.getInsurancePolicy().getCode(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(group -> group.getStaffCategories().getCode(),
                                String.CASE_INSENSITIVE_ORDER))
                .forEach(group -> {
                    InsurancePolicy policy = group.getInsurancePolicy();
                    StaffCategories staffCategory = group.getStaffCategories();
                    InsurancePolicyStaffCategoryGroupResponseDTO policyGroup = groupsByPolicy.computeIfAbsent(
                            policy.getCode(),
                            ignored -> new InsurancePolicyStaffCategoryGroupResponseDTO(
                                    new SimpleBaseDTO(policy.getCode(), policy.getDescription())));
                    policyGroup.getStaffCategories().add(
                            new InsurancePolicyStaffCategoryGroupResponseDTO.StaffCategoryMapping(
                                    staffCategory.getCode(),
                                    staffCategory.getDescription(),
                                    group.getMainCategoryCode(),
                                    group.getMainCategoryDescription()));
                });

        return new ArrayList<>(groupsByPolicy.values());
    }

    private boolean hasActiveReferences(InsurancePolicyStaffCategoryGroup group) {
        return group != null
                && group.getInsurancePolicy() != null
                && Status.ACTIVE.equals(group.getInsurancePolicy().getStatus())
                && group.getInsurancePolicy().getCode() != null
                && group.getStaffCategories() != null
                && Status.ACTIVE.equals(group.getStaffCategories().getStatus())
                && group.getStaffCategories().getCode() != null;
    }
}

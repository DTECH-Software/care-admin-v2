package com.dtech.admin.util;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.InsuranceClaimsDetails;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.InsuranceDetailsLimit;
import com.dtech.admin.model.InsurancePolicy;
import com.dtech.admin.model.InsurancePolicyStaffCategoryGroup;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.InsurancePolicyStaffCategoryGroupRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MedicalClaimStaffCategoryResolver {

    private final InsurancePolicyStaffCategoryGroupRepository insurancePolicyStaffCategoryGroupRepository;
    private final StaffCategoriesRepository staffCategoriesRepository;

    public List<SimpleBaseDTO> loadReferenceCategories() {
        Map<String, String> categories = new LinkedHashMap<>();
        Set<String> mappedActualCodes = new LinkedHashSet<>();

        for (InsurancePolicyStaffCategoryGroup group : insurancePolicyStaffCategoryGroupRepository.findAllByStatus(Status.ACTIVE)) {
            if (group == null) {
                continue;
            }
            String mainCode = normalizeCode(group.getMainCategoryCode());
            String mainDescription = group.getMainCategoryDescription();
            String actualCode = Optional.ofNullable(group.getStaffCategories())
                    .map(StaffCategories::getCode)
                    .map(this::normalizeCode)
                    .orElse(null);

            if (hasText(mainCode)) {
                categories.putIfAbsent(mainCode, mainDescription);
            }
            if (hasText(actualCode)) {
                mappedActualCodes.add(actualCode);
            }
        }

        for (StaffCategories staffCategory : staffCategoriesRepository.findAllByStatus(Status.ACTIVE)) {
            if (staffCategory == null) {
                continue;
            }
            String code = normalizeCode(staffCategory.getCode());
            if (!hasText(code) || mappedActualCodes.contains(code)) {
                continue;
            }
            categories.putIfAbsent(code, staffCategory.getDescription());
        }

        return categories.entrySet().stream()
                .map(entry -> new SimpleBaseDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    public Map<String, String> loadDescriptionMap() {
        Map<String, String> descriptions = new LinkedHashMap<>();

        for (StaffCategories staffCategory : staffCategoriesRepository.findAllByStatus(Status.ACTIVE)) {
            if (staffCategory == null) {
                continue;
            }
            String code = normalizeCode(staffCategory.getCode());
            if (hasText(code)) {
                descriptions.putIfAbsent(code, staffCategory.getDescription());
            }
        }

        for (InsurancePolicyStaffCategoryGroup group : insurancePolicyStaffCategoryGroupRepository.findAllByStatus(Status.ACTIVE)) {
            if (group == null) {
                continue;
            }
            String code = normalizeCode(group.getMainCategoryCode());
            if (hasText(code)) {
                descriptions.putIfAbsent(code, group.getMainCategoryDescription());
            }
        }

        return descriptions;
    }

    public String resolveForClaim(InsuranceClaimsRequest claim) {
        if (claim == null) {
            return null;
        }

        InsurancePolicy policy = Optional.ofNullable(claim.getInsuranceDetailsLimit())
                .map(InsuranceDetailsLimit::getInsurancePolicy)
                .orElseGet(() -> Optional.ofNullable(claim.getEmployee())
                        .map(ApplicationUser::getUserPersonalDetails)
                        .map(UserPersonalDetails::getUserCompanyDetails)
                        .map(UserCompanyDetails::getInsurancePolicy)
                        .orElse(null));

        String staffCategoryCode = Optional.ofNullable(claim.getInsuranceClaimsDetails())
                .map(InsuranceClaimsDetails::getInsuranceStaffCategoryPeriod)
                .map(InsuranceStaffCategoryPeriod::getStaffCategories)
                .map(StaffCategories::getCode)
                .orElseGet(() -> Optional.ofNullable(claim.getEmployee())
                        .map(ApplicationUser::getUserPersonalDetails)
                        .map(UserPersonalDetails::getUserCompanyDetails)
                        .map(UserCompanyDetails::getStaffCategories)
                        .map(StaffCategories::getCode)
                        .orElse(null));

        return resolveForPolicyAndStaff(policy, staffCategoryCode);
    }

    public String resolveForPolicyAndStaff(InsurancePolicy policy, String staffCategoryCode) {
        String normalizedStaffCategoryCode = normalizeCode(staffCategoryCode);
        if (!hasText(normalizedStaffCategoryCode)) {
            return null;
        }

        Long policyId = policy != null ? policy.getId() : null;
        if (policyId == null) {
            return normalizeSelectionCode(normalizedStaffCategoryCode);
        }

        return insurancePolicyStaffCategoryGroupRepository
                .findByInsurancePolicy_IdAndStaffCategories_CodeAndStatus(policyId, normalizedStaffCategoryCode, Status.ACTIVE)
                .map(InsurancePolicyStaffCategoryGroup::getMainCategoryCode)
                .map(this::normalizeCode)
                .filter(this::hasText)
                .orElse(normalizedStaffCategoryCode);
    }

    public String normalizeSelectionCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (!hasText(normalizedCode)) {
            return normalizedCode;
        }

        List<InsurancePolicyStaffCategoryGroup> groups = insurancePolicyStaffCategoryGroupRepository
                .findAllByStaffCategories_CodeAndStatus(normalizedCode, Status.ACTIVE);

        Set<String> mappedCodes = groups.stream()
                .map(InsurancePolicyStaffCategoryGroup::getMainCategoryCode)
                .map(this::normalizeCode)
                .filter(this::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (mappedCodes.size() == 1) {
            return mappedCodes.iterator().next();
        }

        return normalizedCode;
    }

    public List<String> expandActualCodesForFilter(String selectedCode) {
        String normalizedCode = normalizeCode(selectedCode);
        if (!hasText(normalizedCode)) {
            return List.of();
        }

        List<InsurancePolicyStaffCategoryGroup> groups = insurancePolicyStaffCategoryGroupRepository
                .findAllByMainCategoryCodeIgnoreCaseAndStatus(normalizedCode, Status.ACTIVE);

        if (groups.isEmpty()) {
            return List.of(normalizedCode);
        }

        return groups.stream()
                .map(InsurancePolicyStaffCategoryGroup::getStaffCategories)
                .filter(Objects::nonNull)
                .map(StaffCategories::getCode)
                .map(this::normalizeCode)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    public List<String> expandStoredCodesForFilter(String selectedCode) {
        String normalizedCode = normalizeCode(selectedCode);
        if (!hasText(normalizedCode)) {
            return List.of();
        }

        List<String> codes = new ArrayList<>();
        codes.add(normalizedCode);
        codes.addAll(expandActualCodesForFilter(normalizedCode));
        return codes.stream()
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    public String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

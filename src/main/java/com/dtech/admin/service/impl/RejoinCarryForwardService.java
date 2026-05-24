package com.dtech.admin.service.impl;

import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.TreatmentType;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.InsurancePolicy;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.repository.ApplicationUserRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class RejoinCarryForwardService {

    private final ApplicationUserRepository applicationUserRepository;
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Transactional(readOnly = true)
    public BigDecimal getRequestedAmountByTreatment(ApplicationUser currentUser,
                                                    String treatmentCode,
                                                    Long insurancePeriodId,
                                                    InsuranceStaffCategoryPeriod previousPeriod) {
        return resolveRelevantUsers(currentUser).stream()
                .map(user -> sumRequestedAmountByTreatmentForPeriods(user, treatmentCode, insurancePeriodId, previousPeriod))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getRequestedAmountByTreatmentCategory(ApplicationUser currentUser,
                                                            String treatmentCode,
                                                            String categoryCode,
                                                            Long insurancePeriodId,
                                                            InsuranceStaffCategoryPeriod previousPeriod) {
        return resolveRelevantUsers(currentUser).stream()
                .map(user -> sumRequestedAmountByCategoryForPeriods(user, treatmentCode, categoryCode, insurancePeriodId, previousPeriod))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getApprovedAmountByTreatment(ApplicationUser currentUser,
                                                   String treatmentCode,
                                                   Long insurancePeriodId,
                                                   InsuranceStaffCategoryPeriod previousPeriod) {
        return resolveRelevantUsers(currentUser).stream()
                .map(user -> sumApprovedAmountByTreatmentForPeriods(user, treatmentCode, insurancePeriodId, previousPeriod))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getApprovedAmountByTreatmentCategory(ApplicationUser currentUser,
                                                           String treatmentCode,
                                                           String categoryCode,
                                                           Long insurancePeriodId,
                                                           InsuranceStaffCategoryPeriod previousPeriod) {
        return resolveRelevantUsers(currentUser).stream()
                .map(user -> sumApprovedAmountByCategoryForPeriods(user, treatmentCode, categoryCode, insurancePeriodId, previousPeriod))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Date resolveEffectivePermanentDateForLimit(ApplicationUser currentUser) {
        // Rejoin carry-forward should affect utilized amounts only.
        // Entitlement quarter/fund-limit must come from the current profile's inclusion date.
        if (currentUser == null
                || currentUser.getUserPersonalDetails() == null
                || currentUser.getUserPersonalDetails().getUserCompanyDetails() == null) {
            return null;
        }
        return currentUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate();
    }

    private List<ApplicationUser> resolveRelevantUsers(ApplicationUser currentUser) {
        List<ApplicationUser> users = new ArrayList<>();
        if (currentUser == null) {
            return users;
        }

        users.add(currentUser);
        findPreviousInactiveUser(currentUser)
                .filter(previousUser -> isEligibleForCarryForward(currentUser, previousUser))
                .filter(previousUser -> !previousUser.getId().equals(currentUser.getId()))
                .ifPresent(users::add);
        return users;
    }

    private Optional<ApplicationUser> findPreviousInactiveUser(ApplicationUser currentUser) {
        if (currentUser == null
                || currentUser.getId() == null
                || currentUser.getUserPersonalDetails() == null) {
            return Optional.empty();
        }

        String nic = normalize(currentUser.getUserPersonalDetails().getNic());
        if (nic == null) {
            return Optional.empty();
        }

        return applicationUserRepository
                .findAllByUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatusNotAndIdNotOrderByIdDesc(
                        nic,
                        Status.DELETE,
                        currentUser.getId()
                )
                .stream()
                .filter(user -> user.getUserPersonalDetails() != null)
                .filter(user -> Status.INACTIVE.equals(user.getUserPersonalDetails().getUserStatus()))
                .findFirst();
    }

    private boolean isEligibleForCarryForward(ApplicationUser currentUser, ApplicationUser previousUser) {
        String currentEpf = currentUser.getUserPersonalDetails() != null
                ? normalize(currentUser.getUserPersonalDetails().getEpfNo())
                : null;
        String previousEpf = previousUser.getUserPersonalDetails() != null
                ? normalize(previousUser.getUserPersonalDetails().getEpfNo())
                : null;
        if (currentEpf != null && currentEpf.equalsIgnoreCase(previousEpf)) {
            return false;
        }

        InsurancePolicy currentPolicy = currentUser.getUserPersonalDetails() != null
                && currentUser.getUserPersonalDetails().getUserCompanyDetails() != null
                ? currentUser.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy()
                : null;
        InsurancePolicy previousPolicy = previousUser.getUserPersonalDetails() != null
                && previousUser.getUserPersonalDetails().getUserCompanyDetails() != null
                ? previousUser.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy()
                : null;

        String currentPolicyCode = currentPolicy != null ? normalize(currentPolicy.getCode()) : null;
        String previousPolicyCode = previousPolicy != null ? normalize(previousPolicy.getCode()) : null;
        boolean eligible = currentPolicyCode != null
                && currentPolicyCode.equalsIgnoreCase(previousPolicyCode);

        if (eligible) {
            log.info("Admin carry-forward applies. currentUserId={}, previousUserId={}, policy={}",
                    currentUser.getId(), previousUser.getId(), currentPolicyCode);
        }
        return eligible;
    }

    private BigDecimal sumRequestedAmountByTreatmentForPeriods(ApplicationUser user,
                                                               String treatmentCode,
                                                               Long insurancePeriodId,
                                                               InsuranceStaffCategoryPeriod previousPeriod) {
        BigDecimal total = safe(insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                user,
                treatmentCode,
                insurancePeriodId,
                List.of(Workflow.APPROVED)
        ));
        return addPreviousRequestedTreatment(total, user, treatmentCode, insurancePeriodId, previousPeriod);
    }

    private BigDecimal sumRequestedAmountByCategoryForPeriods(ApplicationUser user,
                                                              String treatmentCode,
                                                              String categoryCode,
                                                              Long insurancePeriodId,
                                                              InsuranceStaffCategoryPeriod previousPeriod) {
        BigDecimal total = safe(insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
                user,
                treatmentCode,
                categoryCode,
                insurancePeriodId,
                List.of(Workflow.APPROVED)
        ));

        Long previousPeriodId = shouldApplyPromotionCarryForward(treatmentCode) && previousPeriod != null
                ? previousPeriod.getId()
                : null;
        if (previousPeriodId != null && !previousPeriodId.equals(insurancePeriodId)) {
            total = total.add(safe(insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
                    user,
                    treatmentCode,
                    categoryCode,
                    previousPeriodId,
                    List.of(Workflow.APPROVED)
            )));
        }
        return total;
    }

    private BigDecimal sumApprovedAmountByTreatmentForPeriods(ApplicationUser user,
                                                              String treatmentCode,
                                                              Long insurancePeriodId,
                                                              InsuranceStaffCategoryPeriod previousPeriod) {
        BigDecimal total = safe(insuranceClaimsRequestRepository.getSumApprovedAmountByEmployeeAndTreatmentAndPeriod(
                user,
                treatmentCode,
                insurancePeriodId,
                List.of(Workflow.APPROVED)
        ));

        Long previousPeriodId = shouldApplyPromotionCarryForward(treatmentCode) && previousPeriod != null
                ? previousPeriod.getId()
                : null;
        if (previousPeriodId != null && !previousPeriodId.equals(insurancePeriodId)) {
            total = total.add(safe(insuranceClaimsRequestRepository.getSumApprovedAmountByEmployeeAndTreatmentAndPeriod(
                    user,
                    treatmentCode,
                    previousPeriodId,
                    List.of(Workflow.APPROVED)
            )));
        }
        return total;
    }

    private BigDecimal sumApprovedAmountByCategoryForPeriods(ApplicationUser user,
                                                             String treatmentCode,
                                                             String categoryCode,
                                                             Long insurancePeriodId,
                                                             InsuranceStaffCategoryPeriod previousPeriod) {
        BigDecimal total = safe(insuranceClaimsRequestRepository.getSumApprovedAmountByEmployeeAndTreatmentAndTreatmentCategoryAndPeriod(
                user,
                treatmentCode,
                categoryCode,
                insurancePeriodId,
                List.of(Workflow.APPROVED)
        ));

        Long previousPeriodId = shouldApplyPromotionCarryForward(treatmentCode) && previousPeriod != null
                ? previousPeriod.getId()
                : null;
        if (previousPeriodId != null && !previousPeriodId.equals(insurancePeriodId)) {
            total = total.add(safe(insuranceClaimsRequestRepository.getSumApprovedAmountByEmployeeAndTreatmentAndTreatmentCategoryAndPeriod(
                    user,
                    treatmentCode,
                    categoryCode,
                    previousPeriodId,
                    List.of(Workflow.APPROVED)
            )));
        }
        return total;
    }

    private BigDecimal addPreviousRequestedTreatment(BigDecimal total,
                                                     ApplicationUser user,
                                                     String treatmentCode,
                                                     Long insurancePeriodId,
                                                     InsuranceStaffCategoryPeriod previousPeriod) {
        Long previousPeriodId = shouldApplyPromotionCarryForward(treatmentCode) && previousPeriod != null
                ? previousPeriod.getId()
                : null;
        if (previousPeriodId != null && !previousPeriodId.equals(insurancePeriodId)) {
            total = total.add(safe(insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                    user,
                    treatmentCode,
                    previousPeriodId,
                    List.of(Workflow.APPROVED)
            )));
        }
        return total;
    }

    private boolean shouldApplyPromotionCarryForward(String treatmentCode) {
        return TreatmentType.OUTDOOR.name().equalsIgnoreCase(normalize(treatmentCode));
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

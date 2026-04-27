package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.PaymentAttachmentClaimSearchDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.ThirdPartyIndoorClaimRowStatus;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.*;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
public class PaymentAttachmentClaimSpecification {

    private PaymentAttachmentClaimSpecification() {
    }

    public static Specification<InsuranceClaimsRequest> getSpecification(PaymentAttachmentClaimSearchDTO filterDto) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<InsuranceClaimsDetails, Treatment> treatmentJoin = root.join("insuranceClaimsDetails", JoinType.LEFT)
                    .join("treatment", JoinType.LEFT);

            Join<InsuranceClaimsDetails, TreatmentCategory> treatmentCategoryJoin = root.join("insuranceClaimsDetails", JoinType.LEFT)
                    .join("treatmentCategory", JoinType.LEFT);

            Join<InsuranceClaimsDetails, InsuranceStaffCategoryPeriod> insuranceStaffCategoryPeriodJoin =
                    root.join("insuranceClaimsDetails", JoinType.LEFT)
                            .join("insuranceStaffCategoryPeriod", JoinType.LEFT);

            Join<InsuranceStaffCategoryPeriod, StaffCategories> claimStaffCategoryJoin =
                    insuranceStaffCategoryPeriodJoin.join("staffCategories", JoinType.LEFT);

            Join<InsuranceClaimsRequest, InsuranceDetailsLimit> insuranceDetailsLimitJoin =
                    root.join("insuranceDetailsLimit", JoinType.LEFT);

            Join<InsuranceDetailsLimit, InsurancePolicy> insurancePolicyJoin =
                    insuranceDetailsLimitJoin.join("insurancePolicy", JoinType.LEFT);

            Join<ApplicationUser, UserPersonalDetails> userPersonalDetailsJoin = root.join("employee", JoinType.LEFT)
                    .join("userPersonalDetails", JoinType.LEFT);

            Join<UserPersonalDetails, UserCompanyDetails> companyDetailsJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT);

            Join<UserCompanyDetails, CompanyTypes> companyTypesJoin = companyDetailsJoin.join("companyTypes", JoinType.LEFT);
            Join<UserCompanyDetails, CompanyTypes> paymentCompanyJoin = companyDetailsJoin.join("paymentCompany", JoinType.LEFT);
            if (hasText(filterDto.getClaimId())) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("requestId")), "%" + filterDto.getClaimId().toLowerCase() + "%"));
            }

            if (hasText(filterDto.getClaimCategory())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(treatmentJoin.get("treatmentCode")), filterDto.getClaimCategory().toLowerCase()));
            }

            if (hasText(filterDto.getTreatmentCategory())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(treatmentCategoryJoin.get("code")), filterDto.getTreatmentCategory().toLowerCase()));
            }

            List<String> staffCategoryCodes = filterDto.getStaffCategoryCodes();
            if (staffCategoryCodes != null && !staffCategoryCodes.isEmpty()) {
                staffCategoryCodes = staffCategoryCodes.stream().filter(PaymentAttachmentClaimSpecification::hasText)
                        .map(String::toLowerCase)
                        .toList();
                predicates.add(criteriaBuilder.lower(claimStaffCategoryJoin.get("code")).in(staffCategoryCodes));
                if (hasText(filterDto.getStaffCategory())) {
                    String normalizedRequestedCode = filterDto.getStaffCategory().toLowerCase();

                    Subquery<Long> mappedGroupSubquery = query.subquery(Long.class);
                    Root<InsurancePolicyStaffCategoryGroup> mappedGroupRoot = mappedGroupSubquery.from(InsurancePolicyStaffCategoryGroup.class);
                    mappedGroupSubquery.select(criteriaBuilder.literal(1L));
                    mappedGroupSubquery.where(
                            criteriaBuilder.equal(mappedGroupRoot.get("insurancePolicy").get("id"), insurancePolicyJoin.get("id")),
                            criteriaBuilder.equal(criteriaBuilder.lower(mappedGroupRoot.get("staffCategories").get("code")),
                                    criteriaBuilder.lower(claimStaffCategoryJoin.get("code"))),
                            criteriaBuilder.equal(criteriaBuilder.lower(mappedGroupRoot.get("mainCategoryCode")), normalizedRequestedCode),
                            criteriaBuilder.equal(mappedGroupRoot.get("status"), Status.ACTIVE)
                    );

                    Subquery<Long> anyGroupSubquery = query.subquery(Long.class);
                    Root<InsurancePolicyStaffCategoryGroup> anyGroupRoot = anyGroupSubquery.from(InsurancePolicyStaffCategoryGroup.class);
                    anyGroupSubquery.select(criteriaBuilder.literal(1L));
                    anyGroupSubquery.where(
                            criteriaBuilder.equal(anyGroupRoot.get("insurancePolicy").get("id"), insurancePolicyJoin.get("id")),
                            criteriaBuilder.equal(criteriaBuilder.lower(anyGroupRoot.get("staffCategories").get("code")),
                                    criteriaBuilder.lower(claimStaffCategoryJoin.get("code"))),
                            criteriaBuilder.equal(anyGroupRoot.get("status"), Status.ACTIVE)
                    );

                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.exists(mappedGroupSubquery),
                            criteriaBuilder.and(
                                    criteriaBuilder.not(criteriaBuilder.exists(anyGroupSubquery)),
                                    criteriaBuilder.equal(criteriaBuilder.lower(claimStaffCategoryJoin.get("code")), normalizedRequestedCode)
                            )
                    ));
                }
            } else if (hasText(filterDto.getStaffCategory())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(claimStaffCategoryJoin.get("code")),
                        filterDto.getStaffCategory().toLowerCase()));
            }

            if (hasText(filterDto.getCompany())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(companyTypesJoin.get("code")), filterDto.getCompany().toLowerCase()));
            }

            if (hasText(filterDto.getPaymentCompany())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(paymentCompanyJoin.get("code")), filterDto.getPaymentCompany().toLowerCase()));
            }

            if (hasText(filterDto.getEpf())) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("epfNo")), "%" + filterDto.getEpf().toLowerCase() + "%"));
            }

            if (hasText(filterDto.getDateFrom())) {
                try {
                    Date fromDate = DateTimeUtil.getStartOfDay(normalizeDate(filterDto.getDateFrom()));
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"), fromDate));
                } catch (Exception e) {
                    log.error("Failed to parse from date {}", filterDto.getDateFrom(), e);
                    throw new RuntimeException(e);
                }
            }

            if (hasText(filterDto.getDateTo())) {
                try {
                    Date toDate = DateTimeUtil.getEndOfDay(normalizeDate(filterDto.getDateTo()));
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"), toDate));
                } catch (Exception e) {
                    log.error("Failed to parse to date {}", filterDto.getDateTo(), e);
                    throw new RuntimeException(e);
                }
            }

            List<Workflow> statuses = resolveStatuses(filterDto.getStatus());
            predicates.add(root.get("requestStatus").in(statuses));

            Subquery<Long> importedClaimSubquery = query.subquery(Long.class);
            Root<ThirdPartyIndoorClaimImportRow> importedClaimRoot = importedClaimSubquery.from(ThirdPartyIndoorClaimImportRow.class);
            importedClaimSubquery.select(criteriaBuilder.count(importedClaimRoot));
            importedClaimSubquery.where(
                    criteriaBuilder.equal(importedClaimRoot.get("insuranceClaim"), root),
                    criteriaBuilder.equal(importedClaimRoot.get("status"), ThirdPartyIndoorClaimRowStatus.IMPORTED)
            );
            predicates.add(criteriaBuilder.equal(importedClaimSubquery, 0L));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static List<Workflow> resolveStatuses(List<String> statuses) {
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return List.of(Workflow.APPROVED, Workflow.REJECTED);
        }
        return statuses.stream()
                .filter(PaymentAttachmentClaimSpecification::hasText)
                .map(String::toUpperCase)
                .map(Workflow::valueOf)
                .collect(Collectors.toList());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeDate(String value) {
        if (value.contains("-")) {
            return value.replace("-", "/");
        }
        return value;
    }
}

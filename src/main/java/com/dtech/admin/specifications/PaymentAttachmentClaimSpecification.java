package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.PaymentAttachmentClaimSearchDTO;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.*;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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

            Join<ApplicationUser, UserPersonalDetails> userPersonalDetailsJoin = root.join("employee", JoinType.LEFT)
                    .join("userPersonalDetails", JoinType.LEFT);

            Join<UserPersonalDetails, UserCompanyDetails> companyDetailsJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT);

            Join<UserCompanyDetails, CompanyTypes> companyTypesJoin = companyDetailsJoin.join("companyTypes", JoinType.LEFT);
            Join<UserCompanyDetails, StaffCategories> staffCategoriesJoin = companyDetailsJoin.join("staffCategories", JoinType.LEFT);

            if (hasText(filterDto.getClaimId())) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("requestId")), "%" + filterDto.getClaimId().toLowerCase() + "%"));
            }

            if (hasText(filterDto.getClaimCategory())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(treatmentJoin.get("treatmentCode")), filterDto.getClaimCategory().toLowerCase()));
            }

            if (hasText(filterDto.getTreatmentCategory())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(treatmentCategoryJoin.get("code")), filterDto.getTreatmentCategory().toLowerCase()));
            }

            if (hasText(filterDto.getStaffCategory())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(staffCategoriesJoin.get("code")), filterDto.getStaffCategory().toLowerCase()));
            }

            if (hasText(filterDto.getCompany())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(companyTypesJoin.get("code")), filterDto.getCompany().toLowerCase()));
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

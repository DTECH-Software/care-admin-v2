package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.PaymentAdviceDeathClaimSearchDTO;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.DeathClaimRequest;
import com.dtech.admin.model.PaymentAdviceDeathClaim;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Expression;
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

@Log4j2
public class PaymentAdviceDeathClaimSpecification {

    private PaymentAdviceDeathClaimSpecification() {
    }

    public static Specification<DeathClaimRequest> getSpecification(PaymentAdviceDeathClaimSearchDTO filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<PaymentAdviceDeathClaim> subRoot = subquery.from(PaymentAdviceDeathClaim.class);
            subquery.select(subRoot.get("deathClaim").get("id"))
                    .where(cb.equal(subRoot.get("deathClaim").get("id"), root.get("id")));

            predicates.add(cb.not(cb.exists(subquery)));

            List<Workflow> statusList = resolveStatuses(filterDto.getStatus());
            predicates.add(root.get("requestStatus").in(statusList));

            if (hasText(filterDto.getRequestId())) {
                predicates.add(cb.like(cb.lower(root.get("requestId")),
                        "%" + filterDto.getRequestId().toLowerCase() + "%"));
            }

            Join<DeathClaimRequest, ApplicationUser> employeeJoin = root.join("employee", JoinType.LEFT);
            Join<ApplicationUser, UserPersonalDetails> personalJoin = employeeJoin.join("userPersonalDetails", JoinType.LEFT);
            Join<UserPersonalDetails, UserCompanyDetails> companyJoin = personalJoin.join("userCompanyDetails", JoinType.LEFT);
            Join<UserCompanyDetails, CompanyTypes> paymentCompanyJoin = companyJoin.join("paymentCompany", JoinType.LEFT);
            Join<UserCompanyDetails, CompanyTypes> companyTypeJoin = companyJoin.join("companyTypes", JoinType.LEFT);
            Join<UserCompanyDetails, StaffCategories> staffCategoryJoin = companyJoin.join("staffCategories", JoinType.LEFT);

            Expression<String> effectiveCompany = cb.coalesce(paymentCompanyJoin.get("code"), companyTypeJoin.get("code"));

            if (hasText(filterDto.getPaymentCompany())) {
                predicates.add(cb.equal(cb.lower(effectiveCompany), filterDto.getPaymentCompany().toLowerCase()));
            }

            if (hasText(filterDto.getStaffCategory())) {
                predicates.add(cb.equal(cb.lower(staffCategoryJoin.get("code")), filterDto.getStaffCategory().toLowerCase()));
            }

            if (hasText(filterDto.getDateFrom())) {
                try {
                    Date fromDate = DateTimeUtil.getStartOfDay(normalizeDate(filterDto.getDateFrom()));
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), fromDate));
                } catch (Exception e) {
                    log.error("Failed to parse from date {}", filterDto.getDateFrom(), e);
                    throw new RuntimeException(e);
                }
            }

            if (hasText(filterDto.getDateTo())) {
                try {
                    Date toDate = DateTimeUtil.getEndOfDay(normalizeDate(filterDto.getDateTo()));
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), toDate));
                } catch (Exception e) {
                    log.error("Failed to parse to date {}", filterDto.getDateTo(), e);
                    throw new RuntimeException(e);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
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

    private static List<Workflow> resolveStatuses(List<String> status) {
        if (status == null || status.isEmpty()) {
            return List.of(Workflow.APPROVED, Workflow.REJECTED);
        }
        List<Workflow> resolved = new ArrayList<>();
        for (String raw : status) {
            if (!hasText(raw)) {
                continue;
            }
            resolved.add(Workflow.valueOf(raw.trim()));
        }
        if (resolved.isEmpty()) {
            return List.of(Workflow.APPROVED, Workflow.REJECTED);
        }
        return resolved;
    }
}

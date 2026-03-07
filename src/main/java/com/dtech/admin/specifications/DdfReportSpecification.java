package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.DdfReportSearchDTO;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.DeathClaimRequest;
import com.dtech.admin.model.PaymentAdviceDeathClaim;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class DdfReportSpecification {

    private DdfReportSpecification() {
    }

    public static Specification<DeathClaimRequest> getSpecification(DdfReportSearchDTO search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            Join<DeathClaimRequest, ApplicationUser> employeeJoin = root.join("employee", JoinType.LEFT);
            Join<ApplicationUser, UserPersonalDetails> personalJoin = employeeJoin.join("userPersonalDetails", JoinType.LEFT);
            Join<UserPersonalDetails, UserCompanyDetails> companyJoin = personalJoin.join("userCompanyDetails", JoinType.LEFT);
            Join<UserCompanyDetails, CompanyTypes> companyTypeJoin = companyJoin.join("companyTypes", JoinType.LEFT);
            Join<DeathClaimRequest, ClaimsDependents> dependentJoin = root.join("claimsDependents", JoinType.LEFT);

            if (hasText(search.getCompany())) {
                predicates.add(cb.equal(cb.lower(companyTypeJoin.get("code")), search.getCompany().toLowerCase()));
            }

            if (hasText(search.getRelationCategory())) {
                predicates.add(cb.equal(cb.lower(dependentJoin.get("relationCategory").as(String.class)),
                        search.getRelationCategory().toLowerCase()));
            }

            if (hasText(search.getEmployeeName())) {
                String likeTerm = "%" + search.getEmployeeName().toLowerCase() + "%";
                Expression<String> firstName = cb.lower(personalJoin.get("firstName"));
                Expression<String> lastName = cb.lower(personalJoin.get("lastName"));
                Expression<String> initials = cb.lower(personalJoin.get("initials"));
                predicates.add(cb.or(
                        cb.like(firstName, likeTerm),
                        cb.like(lastName, likeTerm),
                        cb.like(initials, likeTerm)
                ));
            }

            if (hasText(search.getEpfNo())) {
                predicates.add(cb.equal(cb.lower(personalJoin.get("epfNo")), search.getEpfNo().toLowerCase()));
            }

            if (hasText(search.getRequestId())) {
                predicates.add(cb.like(cb.lower(root.get("requestId")),
                        "%" + search.getRequestId().toLowerCase() + "%"));
            }

            if (search.getStatus() != null && !search.getStatus().isEmpty()) {
                List<Workflow> statuses = resolveStatuses(search.getStatus());
                if (!statuses.isEmpty()) {
                    predicates.add(root.get("requestStatus").in(statuses));
                }
            }

            if (hasText(search.getPaymentAdviceStatus())) {
                String adviceStatus = search.getPaymentAdviceStatus().trim().toUpperCase();
                if (adviceStatus.equals("GENERATED") || adviceStatus.equals("NOT_GENERATED")) {
                    boolean generated = adviceStatus.equals("GENERATED");
                    Subquery<Long> subquery = query.subquery(Long.class);
                    Root<PaymentAdviceDeathClaim> subRoot = subquery.from(PaymentAdviceDeathClaim.class);
                    subquery.select(subRoot.get("deathClaim").get("id"))
                            .where(cb.equal(subRoot.get("deathClaim").get("id"), root.get("id")));
                    if (generated) {
                        predicates.add(cb.exists(subquery));
                    } else {
                        predicates.add(cb.not(cb.exists(subquery)));
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static List<Workflow> resolveStatuses(List<String> status) {
        List<Workflow> resolved = new ArrayList<>();
        for (String raw : status) {
            if (!hasText(raw)) {
                continue;
            }
            try {
                resolved.add(Workflow.valueOf(raw.trim()));
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown status {}", raw);
            }
        }
        return resolved;
    }

}

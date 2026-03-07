package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.DdfClaimReportSearchDTO;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.DeathClaimRequest;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
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
public class DdfClaimReportSpecification {

    private DdfClaimReportSpecification() {
    }

    public static Specification<DeathClaimRequest> getSpecification(DdfClaimReportSearchDTO filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<DeathClaimRequest, ApplicationUser> employeeJoin = root.join("employee", JoinType.LEFT);
            Join<ApplicationUser, UserPersonalDetails> personalJoin = employeeJoin.join("userPersonalDetails", JoinType.LEFT);
            Join<UserPersonalDetails, UserCompanyDetails> companyJoin = personalJoin.join("userCompanyDetails", JoinType.LEFT);
            Join<UserCompanyDetails, CompanyTypes> companyTypeJoin = companyJoin.join("companyTypes", JoinType.LEFT);

            if (Objects.nonNull(filterDto)) {
                if (hasText(filterDto.getCompany())) {
                    predicates.add(cb.equal(cb.lower(companyTypeJoin.get("code")), filterDto.getCompany().toLowerCase()));
                }

                List<Workflow> statuses = resolveStatuses(filterDto.getStatus());
                if (statuses.isEmpty()) {
                    statuses = List.of(Workflow.APPROVED, Workflow.REJECTED);
                }
                predicates.add(root.get("requestStatus").in(statuses));

                if (hasText(filterDto.getDateFrom())) {
                    try {
                        Date fromDate = DateTimeUtil.getStartOfDay(normalizeDate(filterDto.getDateFrom()));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), fromDate));
                    } catch (Exception e) {
                        log.error("Failed to parse dateFrom {}", filterDto.getDateFrom(), e);
                        throw new RuntimeException(e);
                    }
                }

                if (hasText(filterDto.getDateTo())) {
                    try {
                        Date toDate = DateTimeUtil.getEndOfDay(normalizeDate(filterDto.getDateTo()));
                        predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), toDate));
                    } catch (Exception e) {
                        log.error("Failed to parse dateTo {}", filterDto.getDateTo(), e);
                        throw new RuntimeException(e);
                    }
                }
            } else {
                predicates.add(root.get("requestStatus").in(List.of(Workflow.APPROVED, Workflow.REJECTED)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<DeathClaimRequest> getSpecification() {
        return (root, query, cb) -> root.get("requestStatus").in(List.of(Workflow.APPROVED, Workflow.REJECTED));
    }

    private static List<Workflow> resolveStatuses(List<String> statuses) {
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return List.of();
        }
        return statuses.stream()
                .filter(DdfClaimReportSpecification::hasText)
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

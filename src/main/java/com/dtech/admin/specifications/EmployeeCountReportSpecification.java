package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.EmployeeCountReportSearchDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EmployeeCountReportSpecification {

    private EmployeeCountReportSpecification() {
    }

    public static Specification<UserPersonalDetails> getSpecification(EmployeeCountReportSearchDTO filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<UserPersonalDetails, UserCompanyDetails> companyDetailsJoin =
                    root.join("userCompanyDetails", JoinType.LEFT);
            Join<UserCompanyDetails, CompanyTypes> companyJoin =
                    companyDetailsJoin.join("companyTypes", JoinType.LEFT);
            Join<UserCompanyDetails, StaffCategories> staffJoin =
                    companyDetailsJoin.join("staffCategories", JoinType.LEFT);

            if (hasText(filterDto.getCompany())) {
                predicates.add(cb.equal(cb.lower(companyJoin.get("code")), filterDto.getCompany().toLowerCase()));
            }

            if (hasText(filterDto.getStaffCategory())) {
                predicates.add(cb.equal(cb.lower(staffJoin.get("code")), filterDto.getStaffCategory().toLowerCase()));
            }

            List<Status> statuses = resolveStatuses(filterDto.getStatus());
            if (statuses.isEmpty()) {
                statuses = List.of(Status.ACTIVE, Status.INACTIVE);
            }
            predicates.add(root.get("userStatus").in(statuses));

            if (filterDto.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"),
                        DateTimeUtil.getStartOfDay(filterDto.getFromDate())));
            }
            if (filterDto.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"),
                        DateTimeUtil.getEndOfDay(filterDto.getToDate())));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<UserPersonalDetails> getSpecification() {
        return (root, query, cb) -> root.get("userStatus").in(List.of(Status.ACTIVE, Status.INACTIVE));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static List<Status> resolveStatuses(List<String> statuses) {
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return List.of();
        }
        return statuses.stream()
                .filter(EmployeeCountReportSpecification::hasText)
                .map(String::toUpperCase)
                .map(Status::valueOf)
                .collect(Collectors.toList());
    }
}

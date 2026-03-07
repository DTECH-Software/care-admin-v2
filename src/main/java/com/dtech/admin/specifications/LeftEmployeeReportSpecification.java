package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.LeftEmployeeReportSearchDTO;
import com.dtech.admin.enums.Facility;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.StaffCategories;
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
public class LeftEmployeeReportSpecification {

    private LeftEmployeeReportSpecification() {
    }

    public static Specification<UserPersonalDetails> getSpecification(LeftEmployeeReportSearchDTO filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<UserPersonalDetails, UserCompanyDetails> companyDetailsJoin =
                    root.join("userCompanyDetails", JoinType.LEFT);
            Join<UserCompanyDetails, CompanyTypes> companyJoin =
                    companyDetailsJoin.join("companyTypes", JoinType.LEFT);
            Join<UserCompanyDetails, StaffCategories> staffJoin =
                    companyDetailsJoin.join("staffCategories", JoinType.LEFT);

            predicates.add(cb.isNotNull(companyDetailsJoin.get("terminateDate")));
            List<Status> statuses = resolveStatuses(filterDto.getStatus());
            if (statuses.isEmpty()) {
                statuses = List.of(Status.ACTIVE, Status.INACTIVE);
            }
            predicates.add(root.get("userStatus").in(statuses));

            if (hasText(filterDto.getFacility())) {
                predicates.add(cb.equal(companyDetailsJoin.get("facility"),
                        Facility.valueOf(filterDto.getFacility().toUpperCase())));
            }
            if (hasText(filterDto.getCompany())) {
                predicates.add(cb.equal(cb.lower(companyJoin.get("code")), filterDto.getCompany().toLowerCase()));
            }
            if (hasText(filterDto.getStaffCategory())) {
                predicates.add(cb.equal(cb.lower(staffJoin.get("code")), filterDto.getStaffCategory().toLowerCase()));
            }

            if (hasText(filterDto.getTerminateDateFrom())) {
                try {
                    Date fromDate = DateTimeUtil.getStartOfDay(normalizeDate(filterDto.getTerminateDateFrom()));
                    predicates.add(cb.greaterThanOrEqualTo(companyDetailsJoin.get("terminateDate"), fromDate));
                } catch (Exception e) {
                    log.error("Failed to parse terminateDateFrom {}", filterDto.getTerminateDateFrom(), e);
                    throw new RuntimeException(e);
                }
            }

            if (hasText(filterDto.getTerminateDateTo())) {
                try {
                    Date toDate = DateTimeUtil.getEndOfDay(normalizeDate(filterDto.getTerminateDateTo()));
                    predicates.add(cb.lessThanOrEqualTo(companyDetailsJoin.get("terminateDate"), toDate));
                } catch (Exception e) {
                    log.error("Failed to parse terminateDateTo {}", filterDto.getTerminateDateTo(), e);
                    throw new RuntimeException(e);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<UserPersonalDetails> getSpecification() {
        return (root, query, cb) -> {
            Join<UserPersonalDetails, UserCompanyDetails> companyDetailsJoin =
                    root.join("userCompanyDetails", JoinType.LEFT);
            return cb.and(
                    cb.isNotNull(companyDetailsJoin.get("terminateDate")),
                    root.get("userStatus").in(List.of(Status.ACTIVE, Status.INACTIVE))
            );
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

    private static List<Status> resolveStatuses(List<String> statuses) {
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return List.of();
        }
        return statuses.stream()
                .filter(LeftEmployeeReportSpecification::hasText)
                .map(String::toUpperCase)
                .map(Status::valueOf)
                .collect(Collectors.toList());
    }
}

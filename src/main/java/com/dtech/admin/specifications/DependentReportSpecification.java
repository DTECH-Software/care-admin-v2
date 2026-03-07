package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.DependentReportSearchDTO;
import com.dtech.admin.enums.DependentCategory;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ClaimsDependents;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
public class DependentReportSpecification {

    private DependentReportSpecification() {
    }

    public static Specification<ClaimsDependents> getSpecification(DependentReportSearchDTO filterDto) {
        return (root, query, criteriaBuilder) -> {
            log.info("Dependent report filter {}", filterDto);
            List<Predicate> predicates = new ArrayList<>();

            Join<ClaimsDependents, ApplicationUser> userJoin = root.join("applicationUser", JoinType.LEFT);
            Join<ApplicationUser, UserPersonalDetails> personalJoin = userJoin.join("userPersonalDetails", JoinType.LEFT);
            Join<UserPersonalDetails, UserCompanyDetails> companyJoin = personalJoin.join("userCompanyDetails", JoinType.LEFT);
            Join<UserCompanyDetails, CompanyTypes> companyTypeJoin = companyJoin.join("companyTypes", JoinType.LEFT);
            Join<UserCompanyDetails, StaffCategories> staffCategoryJoin = companyJoin.join("staffCategories", JoinType.LEFT);

            if (Objects.nonNull(filterDto)) {
                if (hasText(filterDto.getCompany())) {
                    predicates.add(criteriaBuilder.equal(companyTypeJoin.get("code"), filterDto.getCompany()));
                }
                if (hasText(filterDto.getStaffCategory())) {
                    predicates.add(criteriaBuilder.equal(staffCategoryJoin.get("code"), filterDto.getStaffCategory()));
                }
                if (hasText(filterDto.getDependentCategory())) {
                    predicates.add(criteriaBuilder.equal(root.get("dependentCategory"),
                            DependentCategory.valueOf(filterDto.getDependentCategory())));
                }

                List<Workflow> statuses = resolveStatuses(filterDto.getStatus());
                if (!statuses.isEmpty()) {
                    predicates.add(root.get("status").in(statuses));
                }

                if (filterDto.getFromDate() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"),
                            DateTimeUtil.getStartOfDay(filterDto.getFromDate())));
                }
                if (filterDto.getToDate() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"),
                            DateTimeUtil.getEndOfDay(filterDto.getToDate())));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ClaimsDependents> getSpecification() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static List<Workflow> resolveStatuses(List<String> statuses) {
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return List.of();
        }
        return statuses.stream()
                .filter(DependentReportSpecification::hasText)
                .map(String::toUpperCase)
                .map(Workflow::valueOf)
                .collect(Collectors.toList());
    }
}

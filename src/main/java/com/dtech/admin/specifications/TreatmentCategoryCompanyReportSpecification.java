package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.TreatmentCategoryCompanyReportSearchDTO;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsDetails;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.Treatment;
import com.dtech.admin.model.TreatmentCategory;
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

@Log4j2
public class TreatmentCategoryCompanyReportSpecification {

    private TreatmentCategoryCompanyReportSpecification() {
    }

    public static Specification<InsuranceClaimsRequest> getSpecification(TreatmentCategoryCompanyReportSearchDTO filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterDto == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            Join<InsuranceClaimsRequest, InsuranceClaimsDetails> detailsJoin =
                    root.join("insuranceClaimsDetails", JoinType.LEFT);
            Join<InsuranceClaimsDetails, Treatment> treatmentJoin =
                    detailsJoin.join("treatment", JoinType.LEFT);
            Join<InsuranceClaimsDetails, TreatmentCategory> treatmentCategoryJoin =
                    detailsJoin.join("treatmentCategory", JoinType.LEFT);
            Join<InsuranceClaimsRequest, ApplicationUser> employeeJoin =
                    root.join("employee", JoinType.LEFT);
            Join<ApplicationUser, UserPersonalDetails> personalJoin =
                    employeeJoin.join("userPersonalDetails", JoinType.LEFT);
            Join<UserPersonalDetails, UserCompanyDetails> companyJoin =
                    personalJoin.join("userCompanyDetails", JoinType.LEFT);
            Join<UserCompanyDetails, CompanyTypes> companyTypeJoin =
                    companyJoin.join("companyTypes", JoinType.LEFT);
            Join<InsuranceClaimsDetails, InsuranceStaffCategoryPeriod> claimPeriodJoin =
                    detailsJoin.join("insuranceStaffCategoryPeriod", JoinType.LEFT);
            Join<InsuranceStaffCategoryPeriod, StaffCategories> staffCategoryJoin =
                    claimPeriodJoin.join("staffCategories", JoinType.LEFT);

            if (hasText(filterDto.getCompany())) {
                predicates.add(cb.equal(cb.lower(companyTypeJoin.get("code")),
                        filterDto.getCompany().toLowerCase()));
            }

            if (hasText(filterDto.getTreatment())) {
                predicates.add(cb.equal(cb.lower(treatmentJoin.get("treatmentCode")),
                        filterDto.getTreatment().toLowerCase()));
            }

            if (hasText(filterDto.getTreatmentCategory())) {
                predicates.add(cb.equal(cb.lower(treatmentCategoryJoin.get("code")),
                        filterDto.getTreatmentCategory().toLowerCase()));
            }

            if (hasText(filterDto.getStaffCategory())) {
                predicates.add(cb.equal(cb.lower(staffCategoryJoin.get("code")),
                        filterDto.getStaffCategory().toLowerCase()));
            }

            if (hasText(filterDto.getFromDate())) {
                try {
                    Date fromDate = DateTimeUtil.getStartOfDay(normalizeDate(filterDto.getFromDate()));
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), fromDate));
                } catch (Exception e) {
                    log.error("Failed to parse fromDate {}", filterDto.getFromDate(), e);
                    throw new RuntimeException(e);
                }
            }

            if (hasText(filterDto.getToDate())) {
                try {
                    Date toDate = DateTimeUtil.getEndOfDay(normalizeDate(filterDto.getToDate()));
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), toDate));
                } catch (Exception e) {
                    log.error("Failed to parse toDate {}", filterDto.getToDate(), e);
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
}

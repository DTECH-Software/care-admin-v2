package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.EmployeeSummarySearchDTO;
import com.dtech.admin.model.InsuranceClaimsRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class EmployeeSummarySpecification {

    private EmployeeSummarySpecification() {
    }

    public static Specification<InsuranceClaimsRequest> getSpecification(EmployeeSummarySearchDTO searchDTO) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (searchDTO == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (hasText(searchDTO.getCompany())) {
                Join<Object, Object> employee = root.join("employee");
                Join<Object, Object> personal = employee.join("userPersonalDetails");
                Join<Object, Object> companyDetails = personal.join("userCompanyDetails");
                Join<Object, Object> company = companyDetails.join("companyTypes");
                predicates.add(cb.equal(company.get("code"), searchDTO.getCompany()));
            }

            if (hasText(searchDTO.getEpfNo())) {
                Join<Object, Object> employee = root.join("employee");
                Join<Object, Object> personal = employee.join("userPersonalDetails");
                predicates.add(cb.equal(cb.lower(personal.get("epfNo")), searchDTO.getEpfNo().toLowerCase()));
            }

            if (searchDTO.getPeriodId() != null) {
                Join<Object, Object> details = root.join("insuranceClaimsDetails");
                Join<Object, Object> period = details.join("insuranceStaffCategoryPeriod");
                predicates.add(cb.equal(period.get("id"), searchDTO.getPeriodId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

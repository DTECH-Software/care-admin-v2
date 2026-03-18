package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.CivilStatusChangeSearchDTO;

import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Log4j2
public class MaritalSpecification {
    public static Specification<MaritalStatus> getSpecification(CivilStatusChangeSearchDTO filterDto) {
        return getSpecification(filterDto, null);
    }

    public static Specification<MaritalStatus> getSpecification(CivilStatusChangeSearchDTO filterDto, Collection<String> eligibleCompanies) {
        log.info("Claims death approval filter: " + filterDto);
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<MaritalStatus,StaffCategories> staffCategoriesJoin = root.join("applicationUser", JoinType.LEFT)
                    .join("userPersonalDetails",JoinType.LEFT).join("userCompanyDetails", JoinType.LEFT)
                    .join("staffCategories",JoinType.LEFT);

            Join<MaritalStatus,CompanyTypes> comapnayJoin = root.join("applicationUser", JoinType.LEFT)
                    .join("userPersonalDetails",JoinType.LEFT).join("userCompanyDetails", JoinType.LEFT)
                    .join("companyTypes",JoinType.LEFT);

            if (eligibleCompanies != null && !eligibleCompanies.isEmpty()) {
                predicates.add(criteriaBuilder.lower(comapnayJoin.get("code")).in(
                        eligibleCompanies.stream()
                                .map(code -> code.toLowerCase(Locale.ROOT))
                                .toList()
                ));
            }

            if (filterDto.getStaffCategory() != null && !filterDto.getStaffCategory().isEmpty()) {
                predicates.add(criteriaBuilder.equal(staffCategoriesJoin.get("code"),filterDto.getStaffCategory()));
            }

            if (filterDto.getCompany() != null && !filterDto.getCompany().isEmpty()) {
                predicates.add(criteriaBuilder.equal(comapnayJoin.get("code"),filterDto.getCompany()));
            }

            if (filterDto.getCivilStatus() != null && !filterDto.getCivilStatus().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("maritalStatus")), "%" + com.dtech.admin.enums.MaritalStatus.valueOf(filterDto.getCivilStatus())+ "%"));
            }

            if (filterDto.getStatus() != null &&  !filterDto.getStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), Workflow.valueOf(filterDto.getStatus())));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

    public static Specification<MaritalStatus> getSpecification(Collection<String> eligibleCompanies) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<MaritalStatus,CompanyTypes> comapnayJoin = root.join("applicationUser", JoinType.LEFT)
                    .join("userPersonalDetails",JoinType.LEFT).join("userCompanyDetails", JoinType.LEFT)
                    .join("companyTypes",JoinType.LEFT);

            if (eligibleCompanies != null && !eligibleCompanies.isEmpty()) {
                predicates.add(criteriaBuilder.lower(comapnayJoin.get("code")).in(
                        eligibleCompanies.stream()
                                .map(code -> code.toLowerCase(Locale.ROOT))
                                .toList()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}

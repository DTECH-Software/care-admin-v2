package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.ClaimDependentSearchDTO;
import com.dtech.admin.enums.DependentCategory;
import com.dtech.admin.enums.RelationCategory;
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
public class DependentSpecification {
    public static Specification<ClaimsDependents> getSpecification(ClaimDependentSearchDTO filterDto) {
        return getSpecification(filterDto, null);
    }

    public static Specification<ClaimsDependents> getSpecification(ClaimDependentSearchDTO filterDto, Collection<String> eligibleCompanies) {
        log.info("Dependent details filter: " + filterDto);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<ClaimsDependents, UserPersonalDetails> userPersonalDetailsJoin = root.join("applicationUser", JoinType.LEFT)
                    .join("userPersonalDetails", JoinType.LEFT);

            Join<ClaimsDependents, StaffCategories> staffCategoriesJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
                    .join("staffCategories", JoinType.LEFT);

            Join<UserPersonalDetails, CompanyTypes> companyTypesJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
                    .join("companyTypes",JoinType.LEFT);

            if (eligibleCompanies != null && !eligibleCompanies.isEmpty()) {
                predicates.add(criteriaBuilder.lower(companyTypesJoin.get("code")).in(
                        eligibleCompanies.stream()
                                .map(code -> code.toLowerCase(Locale.ROOT))
                                .toList()
                ));
            }

            if (filterDto.getDependentCategory() != null && !filterDto.getDependentCategory().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("dependentCategory"), DependentCategory.valueOf(filterDto.getDependentCategory())));
            }

            if (filterDto.getFirstName() != null && !filterDto.getFirstName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + filterDto.getFirstName().toLowerCase() + "%"));
            }

            if (filterDto.getLastName() != null && !filterDto.getLastName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%" + filterDto.getLastName().toLowerCase() + "%"));
            }

            if (filterDto.getNic() != null && !filterDto.getNic().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("nic")), "%" + filterDto.getNic().toLowerCase() + "%"));
            }

            if (filterDto.getRelationCategory() != null && !filterDto.getRelationCategory().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("relationCategory"), RelationCategory.valueOf(filterDto.getRelationCategory())));
            }

            if (filterDto.getStatus() != null &&  !filterDto.getStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), Workflow.valueOf(filterDto.getStatus())));
            }

            if (filterDto.getLiveStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("liveStatus"), filterDto.getLiveStatus()));
            }

            if (filterDto.getEmployeeNic() != null &&  !filterDto.getEmployeeNic().isEmpty()) {
                predicates.add(criteriaBuilder.equal(userPersonalDetailsJoin.get("nic"), filterDto.getEmployeeNic()));
            }

            if (filterDto.getCompany() != null && !filterDto.getCompany().isEmpty()) {
                predicates.add(criteriaBuilder.equal(companyTypesJoin.get("code"), filterDto.getCompany().toLowerCase()));
            }

            if (filterDto.getStaffCategory() != null && !filterDto.getStaffCategory().isEmpty()) {
                predicates.add(criteriaBuilder.equal(staffCategoriesJoin.get("code"), filterDto.getStaffCategory().toLowerCase()));
            }

            if (filterDto.getEpfNo() != null && !filterDto.getEpfNo().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("epfNo")), "%" + filterDto.getEpfNo().toLowerCase() + "%"));
            }

            if (filterDto.getDependentName() != null && !filterDto.getDependentName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + filterDto.getDependentName().toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

//    public static Specification<UserPersonalDetails> getSpecification() {
//        log.info("Dependent filter default :");
//        return (root, query,criteriaBuilder) -> {
//            List<Predicate> predicates = new ArrayList<>();
//            predicates.add(root.get("userStatus").in(List.of(Status.ACTIVE, Status.INACTIVE)));
//            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
//
//        };
//    }
}

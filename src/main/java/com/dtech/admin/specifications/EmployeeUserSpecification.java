package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.EmployeeSearchDTO;
import com.dtech.admin.enums.Status;
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
public class EmployeeUserSpecification {

    public static Specification<ApplicationUser> getSpecification(EmployeeSearchDTO filterDto) {
        return getSpecification(filterDto, null);
    }

    public static Specification<ApplicationUser> getSpecification(EmployeeSearchDTO filterDto, Collection<String> eligibleCompanies) {
        log.info("Employee user filter: " + filterDto);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<ApplicationUser,UserPersonalDetails> userPersonalDetailsJoin = root.join("userPersonalDetails",JoinType.LEFT);

            Join<UserPersonalDetails, CompanyTypes> compnayTypeJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
                    .join("companyTypes", JoinType.LEFT);

            Join<UserPersonalDetails, StaffCategories> categoriesJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
                    .join("staffCategories", JoinType.LEFT);

            Join<UserPersonalDetails, InsurancePolicy> insurancePolicyJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
                    .join("insurancePolicy", JoinType.LEFT);

            if (eligibleCompanies != null && !eligibleCompanies.isEmpty()) {
                predicates.add(criteriaBuilder.lower(compnayTypeJoin.get("code")).in(
                        eligibleCompanies.stream()
                                .map(code -> code.toLowerCase(Locale.ROOT))
                                .toList()
                ));
            }

            if (filterDto.getEpfNo() != null && !filterDto.getEpfNo().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("epfNo")), "%" + filterDto.getEpfNo().toLowerCase() + "%"));
            }

            if (filterDto.getFirstName() != null && !filterDto.getFirstName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("firstName")), "%" + filterDto.getFirstName().toLowerCase() + "%"));
            }

            if (filterDto.getLastName() != null && !filterDto.getLastName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("lastName")), "%" + filterDto.getLastName().toLowerCase() + "%"));
            }

            if (filterDto.getNic() != null && !filterDto.getNic().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("nic")), "%" + filterDto.getNic().toLowerCase() + "%"));
            }

            if (filterDto.getEmail() != null && !filterDto.getEmail().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("primaryEmail")), "%" + filterDto.getEmail().toLowerCase() + "%"));
            }

            if (filterDto.getUsername() != null && !filterDto.getUsername().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + filterDto.getUsername().toLowerCase() + "%"));
            }

            if (filterDto.getMobileNo() != null && !filterDto.getMobileNo().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("primaryMobile")), "%" + filterDto.getMobileNo().toLowerCase() + "%"));
            }

            if (filterDto.getUserStatus() != null &&  !filterDto.getUserStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(userPersonalDetailsJoin.get("userStatus"), Status.valueOf(filterDto.getUserStatus())));
            }

            if (filterDto.getCompanyCode() != null &&  !filterDto.getCompanyCode().isEmpty()) {
                predicates.add(criteriaBuilder.equal(compnayTypeJoin.get("code"), filterDto.getCompanyCode()));
            }

            if (filterDto.getStaffCategoryCode() != null &&  !filterDto.getStaffCategoryCode().isEmpty()) {
                predicates.add(criteriaBuilder.equal(categoriesJoin.get("code"), filterDto.getStaffCategoryCode()));
            }

            if (filterDto.getInsurancePolicyCode() != null &&  !filterDto.getInsurancePolicyCode().isEmpty()) {
                predicates.add(criteriaBuilder.equal(insurancePolicyJoin.get("code"), filterDto.getInsurancePolicyCode()));
            }

            predicates.add(userPersonalDetailsJoin.get("userStatus").in(List.of(Status.ACTIVE, Status.INACTIVE)));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

    public static Specification<ApplicationUser> getSpecification() {
        return getDefaultSpecification(null);
    }

    public static Specification<ApplicationUser> getSpecification(Collection<String> eligibleCompanies) {
        return getDefaultSpecification(eligibleCompanies);
    }

    private static Specification<ApplicationUser> getDefaultSpecification(Collection<String> eligibleCompanies) {
        log.info("Employee user filter default :");
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<ApplicationUser,UserPersonalDetails> userPersonalDetailsJoin = root.join("userPersonalDetails",JoinType.LEFT);
            Join<UserPersonalDetails, CompanyTypes> compnayTypeJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
                    .join("companyTypes", JoinType.LEFT);

            if (eligibleCompanies != null && !eligibleCompanies.isEmpty()) {
                predicates.add(criteriaBuilder.lower(compnayTypeJoin.get("code")).in(
                        eligibleCompanies.stream()
                                .map(code -> code.toLowerCase(Locale.ROOT))
                                .toList()
                ));
            }

            predicates.add(userPersonalDetailsJoin.get("userStatus").in(List.of(Status.ACTIVE, Status.INACTIVE)));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }
}

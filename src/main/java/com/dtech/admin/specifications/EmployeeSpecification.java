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
import java.util.List;

@Log4j2
public class EmployeeSpecification {
    public static Specification<UserPersonalDetails> getSpecification(EmployeeSearchDTO filterDto) {
        log.info("Employee details filter: " + filterDto);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<UserPersonalDetails, CompanyTypes> compnayTypeJoin = root.join("userCompanyDetails", JoinType.LEFT)
                    .join("companyTypes", JoinType.LEFT);

            Join<UserPersonalDetails, StaffCategories> categoriesJoin = root.join("userCompanyDetails", JoinType.LEFT)
                    .join("staffCategories", JoinType.LEFT);

            Join<UserPersonalDetails, InsurancePolicy> insurancePolicyJoin = root.join("userCompanyDetails", JoinType.LEFT)
                    .join("insurancePolicy", JoinType.LEFT);

            if (filterDto.getEpfNo() != null && !filterDto.getEpfNo().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("epfNo")), "%" + filterDto.getEpfNo().toLowerCase() + "%"));
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

            if (filterDto.getEmail() != null && !filterDto.getEmail().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + filterDto.getEmail().toLowerCase() + "%"));
            }

            if (filterDto.getMobileNo() != null && !filterDto.getMobileNo().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("mobileNo")), "%" + filterDto.getMobileNo().toLowerCase() + "%"));
            }

            if (filterDto.getUserStatus() != null &&  !filterDto.getUserStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("userStatus"), Status.valueOf(filterDto.getUserStatus())));
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

            predicates.add(root.get("userStatus").in(List.of(Status.ACTIVE, Status.INACTIVE)));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

    public static Specification<UserPersonalDetails> getSpecification() {
        log.info("Employee filter default :");
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("userStatus").in(List.of(Status.ACTIVE, Status.INACTIVE)));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }
}

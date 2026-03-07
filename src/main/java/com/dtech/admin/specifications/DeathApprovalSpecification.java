package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
@Log4j2
public class DeathApprovalSpecification {
    public static Specification<DeathClaimRequest> getSpecification(ClaimRequestSearchDTO filterDto, boolean state,boolean both,boolean isEmployee,boolean bothEmp) {
        log.info("Claims death approval filter: " + filterDto);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

//            Join<DeathClaimRequest, UserPersonalDetails> userPersonalDetailsJoin = root.join("employee", JoinType.LEFT)
//                    .join("userPersonalDetails",JoinType.LEFT);

       //     Join<DeathClaimRequest, ClaimsDependents> dependentsJoin = root.join("claimsDependents", JoinType.LEFT);

//            Join<UserPersonalDetails, CompanyTypes> companyTypesJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
//                    .join("companyTypes",JoinType.LEFT);
//
//            Join<UserPersonalDetails, StaffCategories> staffCategoriesJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
//                    .join("staffCategories",JoinType.LEFT);

            if (filterDto.getRequestId() != null && !filterDto.getRequestId().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("requestId")), "%" + filterDto.getRequestId().toLowerCase() + "%"));
            }

            if (filterDto.getRequestStatus() != null && !filterDto.getRequestStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("requestStatus"), Workflow.valueOf(filterDto.getRequestStatus())));
            }

//            if (filterDto.getNic() != null && !filterDto.getNic().isEmpty()) {
//                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("nic")), "%" + filterDto.getNic().toLowerCase() + "%"));
//            }
//            log.info("8");
//            if (filterDto.getEpf() != null && !filterDto.getEpf().isEmpty()) {
//                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("epfNo")), "%" + filterDto.getEpf().toLowerCase() + "%"));
//            }
//            log.info("9");
//            if (filterDto.getEmployeeName() != null && !filterDto.getEmployeeName().isEmpty()) {
//                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("firstName")), "%" + filterDto.getEmployeeName().toLowerCase() + "%"));
//            }
//            log.info("10");
//            if (filterDto.getCompany() != null && !filterDto.getCompany().isEmpty()) {
//                predicates.add(criteriaBuilder.equal(companyTypesJoin.get("code"), filterDto.getCompany().toLowerCase()));
//            }
//            log.info("11");
//            if (filterDto.getStaffCategory() != null && !filterDto.getStaffCategory().isEmpty()) {
//                predicates.add(criteriaBuilder.equal(staffCategoriesJoin.get("code"),  filterDto.getStaffCategory()));
//            }

//            if(isEmployee){
//                predicates.add(criteriaBuilder.isNull(root.get("claimsDependents")));
//            }else{
//                predicates.add(criteriaBuilder.isNotNull(root.get("claimsDependents")));
//            }
//            log.info("13");
//            if(bothEmp){
//                predicates.add(criteriaBuilder.isNull(root.get("claimsDependents")));
//            }

            List<Workflow> wk = new ArrayList<>();
            if(state){
                wk.add(Workflow.APPROVED);
                wk.add(Workflow.REJECTED);
            }else{
                wk.add(Workflow.UNDER_REVIEW);
            }
            if(both){
                wk.add(Workflow.APPROVED);
                wk.add(Workflow.REJECTED);
            }
            predicates.add(root.get("requestStatus").in(wk));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

    public static Specification<DeathClaimRequest> getSpecification(boolean state,boolean both,boolean isEmployee,boolean bothEmp) {
        log.info("Death filter default :");
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<Workflow> wk = new ArrayList<>();
            if(state){
                wk.add(Workflow.APPROVED);
                wk.add(Workflow.REJECTED);
            }else{
                wk.add(Workflow.UNDER_REVIEW);
            }
            if(both){
                wk.add(Workflow.APPROVED);
                wk.add(Workflow.REJECTED);;
            }

            if(bothEmp){
                predicates.add(criteriaBuilder.isNull(root.get("claimsDependents")));
            }

            if(isEmployee){
                predicates.add(criteriaBuilder.isNull(root.get("claimsDependents")));
            }else{
                predicates.add(criteriaBuilder.isNotNull(root.get("claimsDependents")));
            }
            predicates.add(root.get("requestStatus").in(wk));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }
}

package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.*;
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
public class ClaimsApprovalSpecification {
    public static Specification<InsuranceClaimsRequest> getSpecification(ClaimRequestSearchDTO filterDto,boolean state) {
        log.info("Claims approval filter: " + filterDto);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<InsuranceClaimsDetails, Treatment> treatmentJoin = root.join("insuranceClaimsDetails", JoinType.LEFT)
                    .join("treatment", JoinType.LEFT);

            Join<InsuranceClaimsDetails, TreatmentCategory> treatmentCategoryJoin = root.join("insuranceClaimsDetails", JoinType.LEFT)
                    .join("treatmentCategory", JoinType.LEFT);

            Join<ApplicationUser, UserPersonalDetails> userPersonalDetailsJoin = root.join("employee", JoinType.LEFT)
                    .join("userPersonalDetails",JoinType.LEFT);

            Join<UserPersonalDetails, CompanyTypes> companyTypesJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
                    .join("companyTypes",JoinType.LEFT);

            Join<InsuranceClaimsDetails, InsuranceStaffCategoryPeriod> insuranceStaffCategoryPeriodJoin = root.join("insuranceClaimsDetails", JoinType.LEFT)
                    .join("insuranceStaffCategoryPeriod", JoinType.LEFT);

            Join<InsuranceStaffCategoryPeriod, StaffCategories> claimStaffCategoriesJoin =
                    insuranceStaffCategoryPeriodJoin.join("staffCategories", JoinType.LEFT);


            if (filterDto.getFromDate() != null && !filterDto.getFromDate().isEmpty()) {
                try {
                    Date fromDate = DateTimeUtil.getStartOfDay(filterDto.getFromDate());
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"),fromDate));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            if (filterDto.getToDate() != null && !filterDto.getToDate().isEmpty()) {
                try {
                    Date toDate = DateTimeUtil.getEndOfDay(filterDto.getToDate());
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"),toDate));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            if (filterDto.getRequestId() != null && !filterDto.getRequestId().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("requestId")), "%" + filterDto.getRequestId().toLowerCase() + "%"));
            }

            if (filterDto.getRequestStatus() != null && !filterDto.getRequestStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("requestStatus"), Workflow.valueOf(filterDto.getRequestStatus())));
            }

            if (filterDto.getTreatment() != null && !filterDto.getTreatment().isEmpty()) {
                predicates.add(criteriaBuilder.equal(treatmentJoin.get("treatmentCode"),filterDto.getTreatment()));
            }

            if (filterDto.getTreatmentCategory() != null && !filterDto.getTreatmentCategory().isEmpty()) {
                predicates.add(criteriaBuilder.equal(treatmentCategoryJoin.get("code"),filterDto.getTreatmentCategory()));
            }

            if (filterDto.getNic() != null && !filterDto.getNic().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("nic")), "%" + filterDto.getNic().toLowerCase() + "%"));
            }

            if (filterDto.getEpfNo() != null && !filterDto.getEpfNo().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("epfNo")), "%" + filterDto.getEpfNo().toLowerCase() + "%"));
            }

            if (filterDto.getEmployeeName() != null && !filterDto.getEmployeeName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("firstName")), "%" + filterDto.getEmployeeName().toLowerCase() + "%"));
            }

            if (filterDto.getCompany() != null && !filterDto.getCompany().isEmpty()) {
                predicates.add(criteriaBuilder.equal(companyTypesJoin.get("code"), filterDto.getCompany().toLowerCase()));
            }

            if (filterDto.getStaffCategory() != null && !filterDto.getStaffCategory().isEmpty()) {
                predicates.add(criteriaBuilder.equal(claimStaffCategoriesJoin.get("code"),  filterDto.getStaffCategory()));
            }

            if (filterDto.getPeriod() != null) {
                predicates.add(criteriaBuilder.equal(insuranceStaffCategoryPeriodJoin.get("id"),  filterDto.getPeriod()));
            }

            List<Workflow> wk = new ArrayList<>();
            if(state){
                wk.add(Workflow.APPROVED);
                wk.add(Workflow.REJECTED);
            }else{
                wk.add(Workflow.UNDER_REVIEW);
            }
            predicates.add(root.get("requestStatus").in(wk));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

    public static Specification<InsuranceClaimsRequest> getSpecification(boolean state) {
        log.info("Insurance filter default :");
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<Workflow> wk = new ArrayList<>();
            if(state){
                wk.add(Workflow.APPROVED);
                wk.add(Workflow.REJECTED);
            }else{
                wk.add(Workflow.UNDER_REVIEW);
            }
            predicates.add(root.get("requestStatus").in(wk));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }
}

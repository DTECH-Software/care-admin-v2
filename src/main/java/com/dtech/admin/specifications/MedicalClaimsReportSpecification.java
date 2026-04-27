package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.PaymentAttachmentStatus;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsDetails;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.model.PaymentAdviceAttachment;
import com.dtech.admin.model.PaymentAttachment;
import com.dtech.admin.model.PaymentAttachmentClaim;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.Treatment;
import com.dtech.admin.model.TreatmentCategory;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class MedicalClaimsReportSpecification {

    private MedicalClaimsReportSpecification() {
    }

    public static Specification<InsuranceClaimsRequest> getSpecification(ClaimRequestSearchDTO filterDto) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<InsuranceClaimsDetails, Treatment> treatmentJoin = root.join("insuranceClaimsDetails", JoinType.LEFT)
                    .join("treatment", JoinType.LEFT);

            Join<InsuranceClaimsDetails, TreatmentCategory> treatmentCategoryJoin = root.join("insuranceClaimsDetails", JoinType.LEFT)
                    .join("treatmentCategory", JoinType.LEFT);

            Join<ApplicationUser, UserPersonalDetails> userPersonalDetailsJoin = root.join("employee", JoinType.LEFT)
                    .join("userPersonalDetails", JoinType.LEFT);

            Join<UserPersonalDetails, CompanyTypes> companyTypesJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
                    .join("companyTypes", JoinType.LEFT);

            Join<UserPersonalDetails, StaffCategories> staffCategoriesJoin = userPersonalDetailsJoin.join("userCompanyDetails", JoinType.LEFT)
                    .join("staffCategories", JoinType.LEFT);

            Join<InsuranceClaimsDetails, InsuranceStaffCategoryPeriod> insuranceStaffCategoryPeriodJoin =
                    root.join("insuranceClaimsDetails", JoinType.LEFT)
                            .join("insuranceStaffCategoryPeriod", JoinType.LEFT);

            applyFinalDecisionDateFilter(filterDto, root, query, criteriaBuilder, predicates);

            if (filterDto != null && hasText(filterDto.getRequestId())) {
                String requestId = filterDto.getRequestId().trim().toLowerCase();
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("requestId")),
                        "%" + requestId + "%"));
            }

            if (filterDto != null && hasText(filterDto.getRequestStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("requestStatus"),
                        Workflow.valueOf(filterDto.getRequestStatus())));
            }

            if (filterDto != null && hasText(filterDto.getTreatment())) {
                predicates.add(criteriaBuilder.equal(treatmentJoin.get("treatmentCode"), filterDto.getTreatment()));
            }

            if (filterDto != null && hasText(filterDto.getTreatmentCategory())) {
                predicates.add(criteriaBuilder.equal(treatmentCategoryJoin.get("code"), filterDto.getTreatmentCategory()));
            }

            if (filterDto != null && hasText(filterDto.getNic())) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("nic")),
                        "%" + filterDto.getNic().toLowerCase() + "%"));
            }

            if (filterDto != null && hasText(filterDto.getEpfNo())) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("epfNo")),
                        "%" + filterDto.getEpfNo().toLowerCase() + "%"));
            }

            if (filterDto != null && hasText(filterDto.getEmployeeName())) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userPersonalDetailsJoin.get("firstName")),
                        "%" + filterDto.getEmployeeName().toLowerCase() + "%"));
            }

            if (filterDto != null && hasText(filterDto.getCompany())) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(companyTypesJoin.get("code")),
                        filterDto.getCompany().trim().toUpperCase()));
            }

            if (filterDto != null && hasText(filterDto.getStaffCategory())) {
                predicates.add(criteriaBuilder.equal(staffCategoriesJoin.get("code"), filterDto.getStaffCategory()));
            }

            if (filterDto != null && filterDto.getPeriod() != null) {
                predicates.add(criteriaBuilder.equal(insuranceStaffCategoryPeriodJoin.get("id"), filterDto.getPeriod()));
            }

            if (filterDto != null && hasText(filterDto.getPaymentAdviceStatus())) {
                String paymentAdviceStatus = filterDto.getPaymentAdviceStatus().trim().toUpperCase();
                if (paymentAdviceStatus.equals("GENERATED") || paymentAdviceStatus.equals("NOT_GENERATED")) {
                    Subquery<Long> adviceSubquery = query.subquery(Long.class);
                    Root<PaymentAdviceAttachment> adviceRoot = adviceSubquery.from(PaymentAdviceAttachment.class);
                    Join<PaymentAdviceAttachment, PaymentAttachment> adviceAttachmentJoin =
                            adviceRoot.join("paymentAttachment", JoinType.LEFT);
                    Join<PaymentAttachment, PaymentAttachmentClaim> adviceClaimJoin =
                            adviceAttachmentJoin.join("claims", JoinType.LEFT);
                    adviceSubquery.select(adviceRoot.get("id"))
                            .where(criteriaBuilder.equal(adviceClaimJoin.get("insuranceClaimsRequest"), root));
                    if (paymentAdviceStatus.equals("GENERATED")) {
                        predicates.add(criteriaBuilder.exists(adviceSubquery));
                    } else {
                        predicates.add(criteriaBuilder.not(criteriaBuilder.exists(adviceSubquery)));
                    }
                }
            }

            List<Workflow> statuses = new ArrayList<>();
            statuses.add(Workflow.APPROVED);
            statuses.add(Workflow.REJECTED);
            predicates.add(root.get("requestStatus").in(statuses));

            Subquery<Long> settledClaims = query.subquery(Long.class);
            Root<PaymentAttachmentClaim> claimRoot = settledClaims.from(PaymentAttachmentClaim.class);
            Join<PaymentAttachmentClaim, PaymentAttachment> attachmentJoin =
                    claimRoot.join("paymentAttachment", JoinType.LEFT);
            settledClaims.select(claimRoot.get("insuranceClaimsRequest").get("id"))
                    .where(criteriaBuilder.equal(claimRoot.get("insuranceClaimsRequest"), root),
                            criteriaBuilder.equal(attachmentJoin.get("status"), PaymentAttachmentStatus.FINALIZED));
            predicates.add(criteriaBuilder.exists(settledClaims));

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void applyFinalDecisionDateFilter(ClaimRequestSearchDTO filterDto,
                                                     Root<InsuranceClaimsRequest> root,
                                                     CriteriaQuery<?> query,
                                                     CriteriaBuilder criteriaBuilder,
                                                     List<Predicate> predicates) {
        if (filterDto == null || (!hasText(filterDto.getFromDate()) && !hasText(filterDto.getToDate()))) {
            return;
        }

        Subquery<Date> decisionDateSubquery = query.subquery(Date.class);
        Root<InsuranceClaimsRequest> decisionRoot = decisionDateSubquery.from(InsuranceClaimsRequest.class);
        Join<InsuranceClaimsRequest, ApprovalWorkFlow> decisionWorkflowJoin =
                decisionRoot.join("approvalWorkFlows", JoinType.LEFT);

        decisionDateSubquery.select(criteriaBuilder.greatest(decisionWorkflowJoin.<Date>get("approvedDate")))
                .where(
                        criteriaBuilder.equal(decisionRoot.get("id"), root.get("id")),
                        criteriaBuilder.equal(decisionWorkflowJoin.get("status"), root.get("requestStatus")),
                        decisionWorkflowJoin.get("approvalLevel").in(List.of(ApprovalLevel.LEVEL02, ApprovalLevel.LEVEL03)),
                        criteriaBuilder.isNotNull(decisionWorkflowJoin.get("approvedDate"))
                );

        try {
            if (hasText(filterDto.getFromDate())) {
                Date fromDate = DateTimeUtil.getStartOfDay(normalizeDate(filterDto.getFromDate()));
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(decisionDateSubquery, fromDate));
            }

            if (hasText(filterDto.getToDate())) {
                Date toDate = DateTimeUtil.getEndOfDay(normalizeDate(filterDto.getToDate()));
                predicates.add(criteriaBuilder.lessThanOrEqualTo(decisionDateSubquery, toDate));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Specification<InsuranceClaimsRequest> getSpecification() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<Workflow> statuses = new ArrayList<>();
            statuses.add(Workflow.APPROVED);
            statuses.add(Workflow.REJECTED);
            predicates.add(root.get("requestStatus").in(statuses));

            Subquery<Long> settledClaims = query.subquery(Long.class);
            Root<PaymentAttachmentClaim> claimRoot = settledClaims.from(PaymentAttachmentClaim.class);
            Join<PaymentAttachmentClaim, PaymentAttachment> attachmentJoin =
                    claimRoot.join("paymentAttachment", JoinType.LEFT);
            settledClaims.select(claimRoot.get("insuranceClaimsRequest").get("id"))
                    .where(criteriaBuilder.equal(claimRoot.get("insuranceClaimsRequest"), root),
                            criteriaBuilder.equal(attachmentJoin.get("status"), PaymentAttachmentStatus.FINALIZED));
            predicates.add(criteriaBuilder.exists(settledClaims));

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
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

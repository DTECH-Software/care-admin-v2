package com.dtech.admin.dto.audit;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class ClaimsRequestAuditDTO {
    private Long id;
    private String requestId;
    private BigDecimal requestAmount;
    private String requestStatus;
    private String requestStatusDescription;
    private String remark;
    private ApprovalWorkFlowAuditDTO approvalWorkFlow;
    private InsuranceClaimsDetailsAuditDTO insuranceClaimsDetails;
    private DependentDetailsAuditDTO claimsDependents;
    private ApplicationUserAuditDTO employee;
}

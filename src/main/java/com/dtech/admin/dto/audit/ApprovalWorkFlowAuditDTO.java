package com.dtech.admin.dto.audit;

import lombok.Data;

import java.util.Date;

@Data
public class ApprovalWorkFlowAuditDTO {
    private Long id;
    private String approvalLevel;
    private String approvalLevelDescription;
    private Date approvedDate;
    private String approvedUser;
    private String rejectedRemark;
    private String status;
    private String statusDescription;
}

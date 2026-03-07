package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ApprovalWorkFlowResponseDTO {
    private Long id;
    private String approvalLevel;
    private String approvalLevelDescription;
    private Date approvedDate;
    private String approvedUser;
    private String rejectedRemark;
    private String status;
    private String statusDescription;
    private BigDecimal approvedAmount;
}

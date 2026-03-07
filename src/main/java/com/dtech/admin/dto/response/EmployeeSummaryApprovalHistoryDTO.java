package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class EmployeeSummaryApprovalHistoryDTO {
    private String approvalLevel;
    private String approvalLevelDescription;
    private Date approvedDate;
    private String approvedUser;
    private String status;
    private String statusDescription;
    private String rejectedRemark;
    private BigDecimal approvedAmount;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class DeathRequestResponseDTO {
    private Long id;
    private String requestId;
    private BigDecimal utilizeAmount;
    private Date deathDate;
    private String requestStatus;
    private String requestStatusDescription;
    private String remark;
    private List<ApprovalWorkFlowResponseDTO> approvalWorkFlow;
    private List<DocumentDownloadResponseDTO> documents;
    private DependentResponseDTO claimsDependents;
    private ApplicationUserResponseDTO employee;
    private String approvalLevel;
    private String approvalLevelDescription;
    private String paymentType;
    private String paymentTypeDescription;
    private BigDecimal approvedAmount;
    private BigDecimal deathLimit;
    private Date createdDate;
}

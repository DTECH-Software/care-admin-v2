package com.dtech.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Colombo")
    private Date createdDate;
    private String staffCategoryCode;
    private String staffCategoryDescription;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentAttachmentClaimResponseDTO {
    private Long id;
    private Long claimId;
    private String requestId;
    private String employeeName;
    private String epf;
    private String companyCode;
    private String companyDescription;
    private String paymentCompanyCode;
    private String paymentCompanyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private String treatmentCategory;
    private String treatmentCategoryDescription;
    private String claimCategory;
    private String claimCategoryDescription;
    private BigDecimal requestAmount;
    private BigDecimal approvedAmount;
    private String claimStatus;
    private String remark;
}

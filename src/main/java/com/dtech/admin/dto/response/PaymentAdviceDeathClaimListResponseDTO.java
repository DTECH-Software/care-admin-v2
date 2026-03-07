package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentAdviceDeathClaimListResponseDTO {
    private Long id;
    private String requestId;
    private String epf;
    private String employeeName;
    private String dependentName;
    private String relation;
    private String paymentCompanyCode;
    private String paymentCompanyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private BigDecimal approvedAmount;
    private String status;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentAdviceDeathClaimResponseDTO {
    private Long id;
    private Long deathClaimId;
    private String requestId;
    private String epf;
    private String employeeName;
    private String dependentName;
    private String relation;
    private BigDecimal approvedAmount;
    private String claimStatus;
    private String remark;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentAttachmentPrintSummaryDTO {
    private int totalClaims;
    private BigDecimal totalRequestedAmount;
    private BigDecimal totalApprovedAmount;
}

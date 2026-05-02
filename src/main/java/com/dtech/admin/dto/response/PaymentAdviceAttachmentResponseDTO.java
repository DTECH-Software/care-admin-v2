package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentAdviceAttachmentResponseDTO {
    private Long id;
    private Long paymentAttachmentId;
    private String attachmentNo;
    private String companyCode;
    private String companyDescription;
    private String paymentCompanyCode;
    private String paymentCompanyDescription;
    private BigDecimal requestAmount;
    private BigDecimal approvedAmount;
}

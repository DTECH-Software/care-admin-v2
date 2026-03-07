package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentAdviceAttachmentResponseDTO {
    private Long id;
    private Long paymentAttachmentId;
    private String attachmentNo;
    private BigDecimal requestAmount;
    private BigDecimal approvedAmount;
}

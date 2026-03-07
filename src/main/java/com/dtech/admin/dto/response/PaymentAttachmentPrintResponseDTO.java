package com.dtech.admin.dto.response;

import lombok.Data;

@Data
public class PaymentAttachmentPrintResponseDTO {
    private PaymentAttachmentResponseDTO attachment;
    private PaymentAttachmentPrintSummaryDTO summary;
    private String html;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApprovalRejectReasonResponseDTO {
    private Long id;
    private String reasonCode;
    private String reasonDescription;
    private String reasonCategory;
    private BigDecimal amount;
    private String remark;
}

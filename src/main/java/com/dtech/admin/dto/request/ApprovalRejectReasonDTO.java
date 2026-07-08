package com.dtech.admin.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApprovalRejectReasonDTO {
    private String reasonCode;
    private String reasonDescription;
    private String reasonCategory;
    private BigDecimal amount;
    private String remark;
}

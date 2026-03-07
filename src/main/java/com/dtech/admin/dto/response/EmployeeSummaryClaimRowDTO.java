package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class EmployeeSummaryClaimRowDTO {
    private Long id;
    private String requestId;
    private String treatmentType;
    private String treatmentCategory;
    private BigDecimal submittedValue;
    private Date appliedDate;
    private BigDecimal approvedValue;
    private String remark;
}

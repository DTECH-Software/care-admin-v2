package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class EmployeeSummaryClaimInfoDTO {
    private Long id;
    private String treatment;
    private String treatmentCategory;
    private BigDecimal submittedValue;
    private Date appliedDate;
    private BigDecimal approvedValue;
    private String status;
    private String statusDescription;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmployeeSummaryBalanceRowDTO {
    private String treatmentCode;
    private String treatmentDescription;
    private String treatmentCategoryCode;
    private String treatmentCategoryDescription;
    private BigDecimal availableLimit;
    private BigDecimal fundLimit;
}

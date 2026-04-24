package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TreatmentCategoryCompanyReportRowDTO {
    private String companyCode;
    private String companyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private String treatmentCode;
    private String treatmentDescription;
    private String treatmentCategoryCode;
    private String treatmentCategoryDescription;
    private BigDecimal requestTotalAmount;
    private BigDecimal approvedTotalAmount;
    private BigDecimal remainingBalance;
}

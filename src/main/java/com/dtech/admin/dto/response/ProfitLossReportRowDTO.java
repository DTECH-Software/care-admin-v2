package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfitLossReportRowDTO {
    private String companyCode;
    private String companyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private String year;
    private BigDecimal totalPaid;
    private BigDecimal totalReceived;
    private BigDecimal difference;
    private String result;
    private String resultDescription;
}

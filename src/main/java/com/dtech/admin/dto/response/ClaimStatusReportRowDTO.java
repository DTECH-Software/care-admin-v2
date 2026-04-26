package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ClaimStatusReportRowDTO {
    private Date date;
    private String company;
    private String staffCategory;
    private String epfNumber;
    private String employeeName;
    private String dependentName;
    private String dependentCategory;
    private String treatmentType;
    private BigDecimal requestAmount;
    private BigDecimal approvedAmount;
    private String claimStatus;
    private String finalRemark;
}

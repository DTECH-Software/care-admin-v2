package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class EmployeeSummaryClaimInfoDTO {
    private Long id;
    private String treatment;
    private String treatmentCategory;
    private Object submittedValue;
    private Date appliedDate;
    private Object approvedValue;
    private String status;
    private String statusDescription;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class EmployeeSummaryClaimRowDTO {
    private Long id;
    private String requestId;
    private String treatmentType;
    private String treatmentCategory;
    private Object submittedValue;
    private Date appliedDate;
    private Object approvedValue;
    private String remark;
}

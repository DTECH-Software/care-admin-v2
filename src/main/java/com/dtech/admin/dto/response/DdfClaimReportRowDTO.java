package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class DdfClaimReportRowDTO {
    private String epf;
    private String employeeName;
    private String companyName;
    private String relationName;
    private String relation;
    private String relationDescription;
    private String status;
    private String statusDescription;
    private Date deathDate;
    private Date createdDate;
    private java.math.BigDecimal approvedAmount;
}

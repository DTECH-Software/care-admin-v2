package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class DdfReportRowDTO {
    private Long id;
    private String requestId;
    private String companyCode;
    private String companyDescription;
    private String relationCategory;
    private String relationCategoryDescription;
    private String employeeName;
    private String epfNo;
    private String status;
    private String statusDescription;
    private Boolean paymentAdviceGenerated;
    private String paymentAdviceStatusDescription;
    private String chequeNo;
    private Date chequeCreatedDate;
    private java.math.BigDecimal approvedAmount;
    private Date deathDate;
    private Date createdDate;
}

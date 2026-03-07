package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class LeftEmployeeReportRowDTO {
    private Long employeeId;
    private String epf;
    private String employeeName;
    private String companyCode;
    private String companyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private String facility;
    private String facilityDescription;
    private Date terminateDate;
    private String status;
    private String statusDescription;
}

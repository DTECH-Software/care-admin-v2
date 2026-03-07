package com.dtech.admin.dto.response;

import lombok.Data;

@Data
public class EmployeeCountReportRowDTO {
    private String companyCode;
    private String companyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private String status;
    private String statusDescription;
    private long employeeCount;
}

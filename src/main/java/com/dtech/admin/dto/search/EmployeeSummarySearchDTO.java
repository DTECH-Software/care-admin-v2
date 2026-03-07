package com.dtech.admin.dto.search;

import lombok.Data;

@Data
public class EmployeeSummarySearchDTO {
    private String company;
    private Long periodId;
    private String epfNo;
}

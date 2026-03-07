package com.dtech.admin.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class EmployeeReportSearchDTO {
    private String company;
    private String facility;
    private List<String> status;
    private String staffCategory;
    private String permanentDateFrom;
    private String permanentDateTo;
}

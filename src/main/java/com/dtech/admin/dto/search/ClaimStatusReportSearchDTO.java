package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class ClaimStatusReportSearchDTO {
    @JsonAlias({"dateFrom"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String fromDate;

    @JsonAlias({"dateTo"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String toDate;

    private String company;
    private String staffCategory;
    private String epfNo;
    private String employeeName;
    private String dependentName;
    private String dependentCategory;
    private String treatment;
    @JsonAlias({"treatmentCategoryCode"})
    private String treatmentCategory;
    private String claimStatus;
}

package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class DailyTaskReportSearchDTO {
    @JsonAlias({"dateFrom"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String fromDate;

    @JsonAlias({"dateTo"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String toDate;

    private String claimType;
    private String companyCode;

    @JsonAlias({"medicalOtherWork", "medicalOtherWorks"})
    private String medicalOtherWorks;

    @JsonAlias({"ddfOtherWork", "ddfOtherWorks", "deathOtherWorks"})
    private String ddfOtherWorks;
}

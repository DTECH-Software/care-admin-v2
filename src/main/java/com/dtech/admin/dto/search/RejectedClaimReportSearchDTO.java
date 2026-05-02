package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class RejectedClaimReportSearchDTO {
    @JsonAlias({"dateFrom"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String fromDate;

    @JsonAlias({"dateTo"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String toDate;

    private String company;
    private String staffCategory;

    @JsonAlias({"policyPeriod", "policyPeriodId"})
    private String periodId;
}

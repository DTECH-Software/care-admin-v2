package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
public class ProfitLossReportSearchDTO {
    private String company;
    private String staffCategory;
    @JsonAlias({"month"})
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> months;
    @JsonAlias({"chequeYear"})
    private String year;
    @JsonAlias({"type", "claimType"})
    private String reportType;
}

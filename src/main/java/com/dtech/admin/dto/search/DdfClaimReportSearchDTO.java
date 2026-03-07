package com.dtech.admin.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class DdfClaimReportSearchDTO {
    private String company;
    private List<String> status;
    private String dateFrom;
    private String dateTo;
}

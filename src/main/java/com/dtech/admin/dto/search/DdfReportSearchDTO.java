package com.dtech.admin.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class DdfReportSearchDTO {
    private String company;
    private String relationCategory;
    private String employeeName;
    private String epfNo;
    private String requestId;
    private List<String> status;
    private String paymentAdviceStatus;
}

package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class PaymentAdviceDeathSearchDTO {
    private String adviceNo;
    private String paymentCompany;
    private String staffCategory;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateFrom;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateTo;
}

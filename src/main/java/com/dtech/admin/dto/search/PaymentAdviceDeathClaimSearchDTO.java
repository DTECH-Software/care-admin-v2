package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
public class PaymentAdviceDeathClaimSearchDTO {
    private String requestId;
    private String paymentCompany;
    private String staffCategory;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateFrom;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateTo;
    private List<String> status;
}

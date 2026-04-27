package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class PaymentAttachmentClaimSearchDTO {
    private String staffCategory;
    private String claimCategory;
    private String treatmentCategory;
    private String claimId;
    private String company;
    private String paymentCompany;
    private String epf;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateFrom;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateTo;
    @JsonAlias({"statusList"})
    private List<String> status;
    @JsonIgnore
    private List<String> staffCategoryCodes;
}

package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ClaimRequestSearchDTO {
    @SerializedName(value = "requestId", alternate = {"requestID", "requestid"})
    @JsonAlias({"requestID", "requestid"})
    private String requestId;
    private String requestStatus;
    private String treatment;
    private String treatmentCategory;
    private String nic;
    private String dependentNic;
    private String dependentFirstName;
    private String dependentLastName;
    private String staffCategory;
    private Long period;
    @SerializedName(value = "epfNo", alternate = {"epf"})
    @JsonAlias({"epf"})
    private String epfNo;
    private String company;
    private String employeeName;
    @JsonAlias({"paymentAdviceGenerated"})
    private String paymentAdviceStatus;
    @JsonAlias({"dateFrom"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String fromDate;
    @JsonAlias({"dateTo"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String toDate;
}

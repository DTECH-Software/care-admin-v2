package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class ChequePaymentSearchDTO {
    private String company;
    private String staffCategory;
    private String chequeNo;
    private String year;
    @JsonAlias({"month"})
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> months;
    private String chequeDateFrom;
    private String chequeDateTo;
    private String amountFrom;
    private String amountTo;
    @JsonIgnore
    private List<String> staffCategoryCodes;
}

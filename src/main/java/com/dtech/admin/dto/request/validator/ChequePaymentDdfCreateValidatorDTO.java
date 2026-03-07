package com.dtech.admin.dto.request.validator;

import com.dtech.admin.dto.request.ChequePaymentDocumentDTO;
import com.dtech.admin.validator.OnAdd;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChequePaymentDdfCreateValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "Company is required.", groups = {OnAdd.class})
    private String company;
    @NotBlank(message = "Year is required.", groups = {OnAdd.class})
    private String year;
    @NotEmpty(message = "At least one month is required.", groups = {OnAdd.class})
    @JsonAlias({"month"})
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> months;
    @NotBlank(message = "Cheque number is required.", groups = {OnAdd.class})
    private String chequeNo;
    @NotBlank(message = "Cheque bank is required.", groups = {OnAdd.class})
    private String chequeBank;
    @NotBlank(message = "Cheque branch is required.", groups = {OnAdd.class})
    private String chequeBranch;
    private Date chequeDate;
    @NotBlank(message = "Amount is required.", groups = {OnAdd.class})
    private String amount;
    private Date receivedDate;
    @Valid
    private List<ChequePaymentDocumentDTO> documents;
}

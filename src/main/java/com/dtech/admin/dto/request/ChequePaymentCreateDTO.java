package com.dtech.admin.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChequePaymentCreateDTO extends ChannelRequestDTO {
    private String company;
    private String staffCategory;
    private String year;
    @JsonAlias({"month"})
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> months;
    private String chequeNo;
    private String chequeBank;
    private String chequeBranch;
    private Date chequeDate;
    private String amount;
    private Date receivedDate;
    private List<ChequePaymentDocumentDTO> documents;
}

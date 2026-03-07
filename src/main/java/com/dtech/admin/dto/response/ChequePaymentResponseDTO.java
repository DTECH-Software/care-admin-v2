package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ChequePaymentResponseDTO {
    private Long id;
    private String companyCode;
    private String companyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private String year;
    private List<String> months;
    private List<String> monthDescriptions;
    private String chequeNo;
    private String chequeBank;
    private String chequeBranch;
    private Date chequeDate;
    private BigDecimal amount;
    private Date receivedDate;
    private List<ChequePaymentDocumentResponseDTO> documents;
}

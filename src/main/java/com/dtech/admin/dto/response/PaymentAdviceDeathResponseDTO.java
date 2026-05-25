package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class PaymentAdviceDeathResponseDTO {
    private Long id;
    private String adviceNo;
    private Integer adviceYearStart;
    private Integer adviceYearEnd;
    private Integer adviceSequence;
    private String voucherNo;
    private Integer voucherSequence;
    private String chequeNo;
    private String originalCompanyCode;
    private String originalCompanyDescription;
    private String paymentCompanyCode;
    private String paymentCompanyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private String department;
    private BigDecimal totalApprovedAmount;
    private BigDecimal totalRequestedAmount;
    private String status;
    private Date createdDate;
    private String createdBy;
    private Date lastModifiedDate;
    private String lastModifiedBy;
    private List<PaymentAdviceDeathClaimResponseDTO> claims;
}

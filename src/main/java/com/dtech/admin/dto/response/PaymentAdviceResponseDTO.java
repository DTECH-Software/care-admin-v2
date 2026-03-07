package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class PaymentAdviceResponseDTO {
    private Long id;
    private String adviceNo;
    private Integer adviceYearStart;
    private Integer adviceYearEnd;
    private Integer adviceSequence;
    private String voucherNo;
    private Integer voucherSequence;
    private String companyCode;
    private String companyDescription;
    private String paymentCompanyCode;
    private String paymentCompanyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private String department;
    private BigDecimal totalRequestedAmount;
    private BigDecimal totalApprovedAmount;
    private String status;
    private Date createdDate;
    private String createdBy;
    private Date lastModifiedDate;
    private String lastModifiedBy;
    private List<PaymentAdviceAttachmentResponseDTO> attachments;
}

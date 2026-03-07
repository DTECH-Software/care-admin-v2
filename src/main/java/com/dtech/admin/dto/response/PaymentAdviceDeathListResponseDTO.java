package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentAdviceDeathListResponseDTO {
    private Long id;
    private String adviceNo;
    private String voucherNo;
    private String chequeNo;
    private String paymentCompanyCode;
    private String paymentCompanyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private BigDecimal totalApprovedAmount;
    private String status;
    private Date createdDate;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentAdviceListResponseDTO {
    private Long id;
    private String adviceNo;
    private String voucherNo;
    private String companyCode;
    private String companyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private BigDecimal totalRequestedAmount;
    private BigDecimal totalApprovedAmount;
    private String status;
    private Date createdDate;
    private String createdBy;
}

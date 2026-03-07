package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class PaymentAttachmentListResponseDTO {
    private Long id;
    private String attachmentNo;
    private String status;
    private String companyCode;
    private String companyDescription;
    private String paymentCompanyCode;
    private String paymentCompanyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
    private String treatmentCategory;
    private String treatmentCategoryDescription;
    private Date dateFrom;
    private Date dateTo;
    private Date createdDate;
    private String createdBy;
}

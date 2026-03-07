package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PaymentAttachmentResponseDTO {
    private Long id;
    private String attachmentNo;
    private String attachmentPrefix;
    private Integer attachmentYear;
    private Integer attachmentSequence;
    private String notes;
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
    private String status;
    private Date createdDate;
    private String createdBy;
    private Date lastModifiedDate;
    private String lastModifiedBy;
    private List<PaymentAttachmentClaimResponseDTO> claims;
}

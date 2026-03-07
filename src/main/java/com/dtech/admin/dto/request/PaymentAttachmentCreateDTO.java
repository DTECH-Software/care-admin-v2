package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAttachmentCreateDTO extends ChannelRequestDTO {
    private String attachmentPrefix;
    private String notes;
    private String companyCode;
    private String staffCategoryCode;
    private String treatmentCategory;
    private String dateFrom;
    private String dateTo;
    private List<Long> claimIds;
}

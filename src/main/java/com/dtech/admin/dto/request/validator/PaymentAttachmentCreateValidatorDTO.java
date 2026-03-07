package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnAdd;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAttachmentCreateValidatorDTO extends ChannelRequestValidatorDTO {
    private String attachmentPrefix;
    private String notes;
    private String companyCode;
    private String staffCategoryCode;
    private String treatmentCategory;
    private String dateFrom;
    private String dateTo;
    @NotNull(message = "Claim ID list is required", groups = {OnAdd.class})
    @NotEmpty(message = "At least one claim must be selected", groups = {OnAdd.class})
    private List<Long> claimIds;
}

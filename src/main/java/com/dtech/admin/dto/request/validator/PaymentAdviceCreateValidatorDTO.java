package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnAdd;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAdviceCreateValidatorDTO extends ChannelRequestValidatorDTO {
    private String companyCode;
    private String staffCategoryCode;
    @NotNull(message = "Attachment ID list is required", groups = {OnAdd.class})
    @NotEmpty(message = "At least one attachment must be selected", groups = {OnAdd.class})
    private List<Long> attachmentIds;
}

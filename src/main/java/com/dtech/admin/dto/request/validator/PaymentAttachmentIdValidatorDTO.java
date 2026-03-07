package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnGet;
import com.dtech.admin.validator.OnUpdate;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAttachmentIdValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Attachment ID is required", groups = {OnGet.class, OnUpdate.class})
    private Long id;
}

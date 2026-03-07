package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAttachmentRejectValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Attachment ID is required", groups = {OnUpdate.class})
    private Long id;
    @NotBlank(message = "Rejection remark is required", groups = {OnUpdate.class})
    private String remark;
}

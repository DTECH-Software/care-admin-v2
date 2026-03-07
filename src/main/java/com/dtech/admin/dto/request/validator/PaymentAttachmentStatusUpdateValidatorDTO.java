package com.dtech.admin.dto.request.validator;

import com.dtech.admin.enums.PaymentAttachmentStatus;
import com.dtech.admin.validator.Conditional;
import com.dtech.admin.validator.OnUpdate;
import com.dtech.admin.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Conditional(selected = "status",
        values = {"REJECTED"},
        required = {"remark"},
        message = "Rejection remark is required",
        groups = {OnUpdate.class})
@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAttachmentStatusUpdateValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Attachment ID is required", groups = {OnUpdate.class})
    private Long id;

    @NotBlank(message = "Status is required", groups = {OnUpdate.class})
    @ValidEnum(enumClass = PaymentAttachmentStatus.class, message = "Invalid status", groups = {OnUpdate.class})
    private String status;

    private String remark;
}

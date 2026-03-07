package com.dtech.admin.dto.request.validator;

import com.dtech.admin.enums.AuthorizerStatus;
import com.dtech.admin.validator.Conditional;
import com.dtech.admin.validator.OnGet;
import com.dtech.admin.validator.ValidEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Conditional(selected = "status",
        values = {"REJECTED"}, required = {"rejectReason"}, message = "Reject reason is required.")
@Conditional(selected = "message",
        values = {"FINANCIAL_APP"}, required = {"status"}, message = "Authorizer status is required.")
public class UploaderRejectRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "ID is required",groups = {OnGet.class})
    private Long id;
    @ValidEnum(enumClass = AuthorizerStatus.class, message = "Invalid authorizer status.")
    private String status;
    private String rejectReason;
}

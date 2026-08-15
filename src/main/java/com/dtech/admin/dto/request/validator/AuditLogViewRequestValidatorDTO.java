package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnGet;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogViewRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Audit log ID is required", groups = OnGet.class)
    private Long id;
}

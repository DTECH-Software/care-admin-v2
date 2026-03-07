package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnGet;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAdviceIdValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Payment advice id is required", groups = {OnGet.class})
    private Long id;
}

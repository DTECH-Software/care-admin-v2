package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnAdd;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAdviceDeathCreateValidatorDTO extends ChannelRequestValidatorDTO {
    private String paymentCompanyCode;
    private String staffCategoryCode;
    @NotEmpty(message = "Death claim ids are required", groups = {OnAdd.class})
    private List<Long> deathClaimIds;
}

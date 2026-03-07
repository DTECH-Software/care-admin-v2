package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAdviceDeathCreateDTO extends ChannelRequestDTO {
    private String paymentCompanyCode;
    private String staffCategoryCode;
    private List<Long> deathClaimIds;
}

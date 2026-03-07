package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAdviceCreateDTO extends ChannelRequestDTO {
    private String companyCode;
    private String staffCategoryCode;
    private List<Long> attachmentIds;
}

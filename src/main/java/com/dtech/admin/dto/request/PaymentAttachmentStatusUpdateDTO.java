package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentAttachmentStatusUpdateDTO extends ChannelRequestDTO {
    private Long id;
    private String status;
    private String remark;
}

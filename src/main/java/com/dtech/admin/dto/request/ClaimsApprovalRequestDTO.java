package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClaimsApprovalRequestDTO extends ChannelRequestDTO{
    private Long id;
    private String status;
}

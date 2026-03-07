package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UploaderRejectDTO extends ChannelRequestDTO{
    private Long id;
    private String status;
    private String rejectReason;
}

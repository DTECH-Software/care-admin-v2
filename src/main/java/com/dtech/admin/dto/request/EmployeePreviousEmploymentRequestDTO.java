package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePreviousEmploymentRequestDTO extends ChannelRequestDTO {
    private String nic;
}

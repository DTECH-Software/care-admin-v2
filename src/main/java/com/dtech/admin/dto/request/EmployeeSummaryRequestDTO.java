package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeSummaryRequestDTO extends ChannelRequestDTO {
    private String company;
    private Long periodId;
    private String epfNo;
}

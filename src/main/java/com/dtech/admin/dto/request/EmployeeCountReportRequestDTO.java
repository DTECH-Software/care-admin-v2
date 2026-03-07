package com.dtech.admin.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeCountReportRequestDTO extends ChannelRequestDTO {
    @JsonAlias({"companyCode"})
    private String company;
    @JsonAlias({"staffCategoryCode"})
    private String staffCategory;
    private String status;
}

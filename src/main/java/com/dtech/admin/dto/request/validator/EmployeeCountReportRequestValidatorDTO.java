package com.dtech.admin.dto.request.validator;

import com.dtech.admin.enums.Status;
import com.dtech.admin.validator.OnGet;
import com.dtech.admin.validator.ValidEnum;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeCountReportRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @JsonAlias({"companyCode"})
    @NotBlank(message = "Company is required", groups = {OnGet.class})
    private String company;
    @JsonAlias({"staffCategoryCode"})
    @NotBlank(message = "Staff category is required", groups = {OnGet.class})
    private String staffCategory;
    @NotBlank(message = "Status is required", groups = {OnGet.class})
    @ValidEnum(enumClass = Status.class, message = "Status is invalid", groups = {OnGet.class})
    private String status;
}

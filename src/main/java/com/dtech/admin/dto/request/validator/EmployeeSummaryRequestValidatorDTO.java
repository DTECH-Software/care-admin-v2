package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnGet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeSummaryRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "Company is required.", groups = {OnGet.class})
    private String company;
    @NotNull(message = "Policy period is required.", groups = {OnGet.class})
    private Long periodId;
    @NotBlank(message = "EPF number is required.", groups = {OnGet.class})
    private String epfNo;
}

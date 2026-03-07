package com.dtech.admin.dto.request.validator;

import com.dtech.admin.enums.Month;
import com.dtech.admin.enums.WorkBookType;
import com.dtech.admin.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class ExcelUploadValidatorRequestDTO extends ChannelRequestValidatorDTO{
    @NotBlank(message = "Company is required")
    private String company;
    @NotBlank(message = "Work book is required")
    @ValidEnum(enumClass = WorkBookType.class, message = "Invalid work book type.")
    private String workBookType;
    @NotBlank(message = "Year is required")
    private String year;
    @NotBlank(message = "Month is required")
    @ValidEnum(enumClass = Month.class, message = "Invalid month.")
    private String month;
}
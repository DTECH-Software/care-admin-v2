package com.dtech.admin.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePreviousEmploymentRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "NIC is required")
    @Pattern(regexp = "^[0-9]{9}[Vv]?$|^[0-9]{12}$",
            message = "Invalid NIC number. It must be 9 digits optionally followed by 'V' or 'v', or exactly 12 digits")
    private String nic;
}

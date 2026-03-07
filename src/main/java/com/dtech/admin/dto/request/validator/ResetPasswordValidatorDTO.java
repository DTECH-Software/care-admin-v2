package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.PasswordEquals;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@PasswordEquals(message = "New password and confirm password mismatch.Please try again")
public class ResetPasswordValidatorDTO extends ChannelRequestValidatorDTO{
    @NotBlank(message = "New password cannot be empty")
    private String password;
    @NotBlank(message = "Confirm password cannot be empty")
    private String confirmPassword;
}
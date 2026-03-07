/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 8:29 AM
 * <p>
 */

package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnUpdate;
import com.dtech.admin.validator.ValidCommonPolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ValidCommonPolicy(message = "Password policy invalid",groups = {OnUpdate.class})
public class PasswordPolicyRequestValidatorDTO extends CommonPolicyRequestValidatorDTO {
    @NotNull(message = "Password history is required",groups = {OnUpdate.class})
    @Min(value = 0, message = "Password history must be zero or a positive number", groups = {OnUpdate.class})
    private int passwordHistory = 0;
    @NotNull(message = "Attempt exceed count is required",groups = {OnUpdate.class})
    @Min(value = 0, message = "Attempt exceed count must be zero or a positive number", groups = {OnUpdate.class})
    private int attemptExceedCount = 0;
    @NotNull(message = "OTP exceed count is required",groups = {OnUpdate.class})
    @Min(value = 0, message = "OTP exceed count must be zero or a positive number", groups = {OnUpdate.class})
    private int otpExceedCount = 0;
}

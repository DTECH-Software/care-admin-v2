/**
 * User: Himal_J
 * Date: 4/24/2025
 * Time: 1:03 PM
 * <p>
 */

package com.dtech.admin.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoginRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotBlank(message = "Password is required.")
    private String password;
}

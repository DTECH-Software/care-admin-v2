/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 1:30 PM
 * <p>
 */

package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnUpdate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommonPolicyRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotNull(message = "Id is required",groups = {OnUpdate.class})
    private Long id;
    @NotNull(message = "Min upper case is required",groups = {OnUpdate.class})
    @Min(value = 0, message = "Min upper case must be zero or a positive number", groups = {OnUpdate.class})
    private int minUpperCase = 0;
    @NotNull(message = "Min lower case is required",groups = {OnUpdate.class})
    @Min(value = 0, message = "Min lower case  must be zero or a positive number", groups = {OnUpdate.class})
    private int minLowerCase = 0;
    @NotNull(message = "Min number is required",groups = {OnUpdate.class})
    @Min(value = 0, message = "Min numbers  must be zero or a positive number", groups = {OnUpdate.class})
    private int minNumbers = 0;
    @NotNull(message = "Min special characters is required",groups = {OnUpdate.class})
    @Min(value = 0, message = "Min special characters  must be zero or a positive number", groups = {OnUpdate.class})
    private int minSpecialCharacters  = 0;
    @NotNull(message = "Min length is required",groups = {OnUpdate.class})
    @Min(value = 1, message = "Min length  must be one or a positive number", groups = {OnUpdate.class})
    private int minLength = 1;
    @NotNull(message = "Max length is required",groups = {OnUpdate.class})
    @Min(value = 1, message = "Max length  must be one or a positive number", groups = {OnUpdate.class})
    private int maxLength = 1;
}

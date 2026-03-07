/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 3:46 PM
 * <p>
 */

package com.dtech.admin.dto.request.validator;

import com.dtech.admin.enums.Status;
import com.dtech.admin.validator.OnDelete;
import com.dtech.admin.validator.OnGet;
import com.dtech.admin.validator.OnUpdate;
import com.dtech.admin.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SectionRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "ID is required",groups = {OnGet.class, OnUpdate.class, OnDelete.class})
    private Long id;
    @NotBlank(message = "Status is required",groups = {OnUpdate.class})
    @ValidEnum(enumClass = Status.class, message = "Invalid status",groups = {OnUpdate.class})
    private String status;
}

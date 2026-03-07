package com.dtech.admin.dto.request.validator;

import com.dtech.admin.enums.Workflow;
import com.dtech.admin.validator.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Conditional(selected = "status",
        values = {
                "REJECTED",
        }, required = {"remark"}, message = "Remark is required",groups = {OnUpdate.class})
public class DependentRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "ID is required",groups = {OnGet.class, OnUpdate.class})
    private Long id;
    @NotBlank(message = "Status is required",groups = {OnUpdate.class})
    @ValidEnum(enumClass = Workflow.class, message = "Status is invalid",groups = {OnUpdate.class})
    private String status;
    private String remark;
}

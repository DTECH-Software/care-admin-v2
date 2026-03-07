/**
 * User: Himal_J
 * Date: 4/27/2025
 * Time: 7:21 PM
 * <p>
 */

package com.dtech.admin.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
//@Conditional(selected = "message",
//        values = {
//                "ASSIGNED_TASK",
//        }, required = {"assignedTask"}, message = "Assigned task is required.")
public class AssignedUnAssignedTaskRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "Page code is required.")
    private String page;
    @NotBlank(message = "User role code is required.")
    private String userRole;
    private List<String> assignedTask = new ArrayList<>();
}

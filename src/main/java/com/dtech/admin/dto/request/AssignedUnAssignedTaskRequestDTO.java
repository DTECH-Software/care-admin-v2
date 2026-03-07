/**
 * User: Himal_J
 * Date: 4/27/2025
 * Time: 6:04 PM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class AssignedUnAssignedTaskRequestDTO extends ChannelRequestDTO{
    private String page;
    private String userRole;
    private List<String> assignedTask;
}

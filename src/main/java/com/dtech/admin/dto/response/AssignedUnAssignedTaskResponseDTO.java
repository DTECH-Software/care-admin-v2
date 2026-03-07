/**
 * User: Himal_J
 * Date: 4/27/2025
 * Time: 6:49 PM
 * <p>
 */

package com.dtech.admin.dto.response;

import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedUnAssignedTaskResponseDTO {
    private List<SimpleBaseDTO> assignedTask;
    private List<SimpleBaseDTO> unAssignedTask;
}

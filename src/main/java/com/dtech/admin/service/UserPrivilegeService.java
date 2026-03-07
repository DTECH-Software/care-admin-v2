/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 11:50 AM
 * <p>
 */

package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.AssignedUnAssignedTaskRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface UserPrivilegeService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> getTaskByPageAndUserRole(AssignedUnAssignedTaskRequestDTO assignedUnAssignedTaskRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> assignedPrivileges(AssignedUnAssignedTaskRequestDTO assignedUnAssignedTaskRequestDTO, Locale locale);
}

/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 12:00 PM
 * <p>
 */

package com.dtech.admin.service;

import com.dtech.admin.dto.search.CommonSearchDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.UserRoleRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;


public interface UserRoleService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> add(UserRoleRequestDTO userRoleRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(UserRoleRequestDTO userRoleRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> update(UserRoleRequestDTO userRoleRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> delete(UserRoleRequestDTO userRoleRequestDTO, Locale locale);
}

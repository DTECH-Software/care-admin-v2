/**
 * User: Himal_J
 * Date: 4/30/2025
 * Time: 10:30 AM
 * <p>
 */

package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.SystemUserRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.SystemUserSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface SystemUserService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<SystemUserSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> add(SystemUserRequestDTO systemUserRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(SystemUserRequestDTO systemUserRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> update(SystemUserRequestDTO systemUserRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> delete(SystemUserRequestDTO systemUserRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> sentOTP(SystemUserRequestDTO systemUserRequestDTO, Locale locale);
}

/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 12:00 PM
 * <p>
 */

package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.UsernamePolicyRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;


public interface UsernamePolicyService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(UsernamePolicyRequestDTO usernamePolicyRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> update(UsernamePolicyRequestDTO usernamePolicyRequestDTO, Locale locale);
}

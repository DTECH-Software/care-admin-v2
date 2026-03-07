package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.LoginRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface LoginService {
    ResponseEntity<ApiResponse<Object>> login(LoginRequestDTO loginRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> leftMenu(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> logout(ChannelRequestDTO channelRequestDTO, Locale locale);
}

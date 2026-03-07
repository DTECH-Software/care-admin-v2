package com.dtech.admin.service;


import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ResetPasswordDTO;
import com.dtech.admin.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ResetPasswordService {
   ResponseEntity<ApiResponse<Object>> resetPassword(ResetPasswordDTO resetPasswordDTO, Locale locale);
   ResponseEntity<ApiResponse<Object>> requestOTP(ChannelRequestDTO channelRequestDTO, Locale locale);
}

package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface DashboardService {
    ResponseEntity<ApiResponse<Object>> getSummary(ChannelRequestDTO channelRequestDTO, Locale locale);
}

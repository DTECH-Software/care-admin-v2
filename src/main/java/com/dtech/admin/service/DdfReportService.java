package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DdfReportRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.DdfReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface DdfReportService {

    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);

    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<DdfReportSearchDTO> paginationRequest, Locale locale);

    ResponseEntity<ApiResponse<Object>> view(DdfReportRequestDTO requestDTO, Locale locale);

    ResponseEntity<byte[]> export(PaginationRequest<DdfReportSearchDTO> paginationRequest, Locale locale);
}

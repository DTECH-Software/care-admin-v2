package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.TreatmentCategoryCompanyReportSearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface TreatmentCategoryCompanyReportService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<TreatmentCategoryCompanyReportSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<byte[]> export(PaginationRequest<TreatmentCategoryCompanyReportSearchDTO> paginationRequest, Locale locale);
}

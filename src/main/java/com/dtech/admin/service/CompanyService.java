/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 12:00 PM
 * <p>
 */

package com.dtech.admin.service;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.CompanyRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.CompanySearchDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;


public interface CompanyService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<CompanySearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> add(CompanyRequestDTO companyRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(CompanyRequestDTO companyRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> update(CompanyRequestDTO companyRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> delete(CompanyRequestDTO companyRequestDTO, Locale locale);
}

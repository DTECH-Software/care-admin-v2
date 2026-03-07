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
import com.dtech.admin.dto.request.SectionRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;


public interface SectionService {
    ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> view(SectionRequestDTO sectionRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> update(SectionRequestDTO sectionRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> delete(SectionRequestDTO sectionRequestDTO, Locale locale);
}

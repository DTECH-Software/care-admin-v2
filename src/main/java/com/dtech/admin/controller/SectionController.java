/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 12:28 PM
 * <p>
 */

package com.dtech.admin.controller;
import com.dtech.admin.dto.search.CommonSearchDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.SectionRequestDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.SectionRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.service.SectionService;
import com.dtech.admin.validator.OnDelete;
import com.dtech.admin.validator.OnGet;
import com.dtech.admin.validator.OnUpdate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/section")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SectionController {

    @Autowired
    private final SectionService sectionService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle section reference data find request request ",notes = "Section reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Section reference data request reference data controller {} ", channelRequestValidatorDTO);
        return sectionService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle sections filter list request request ",notes = "Sections filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale) {
        log.info("Sections filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<CommonSearchDTO>>(){}.getType();
        return sectionService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle sections find by ID request request ",notes = "Sections find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid SectionRequestValidatorDTO sectionRequestValidatorDTO, Locale locale) {
        log.info("Sections find by ID request controller {} ", sectionRequestValidatorDTO);
        return sectionService.view(gson.fromJson(gson.toJson(sectionRequestValidatorDTO), SectionRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle sections update request request ",notes = "Sections update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid SectionRequestValidatorDTO sectionRequestValidatorDTO, Locale locale) {
        log.info("Sections update request controller {} ", sectionRequestValidatorDTO);
        return sectionService.update(gson.fromJson(gson.toJson(sectionRequestValidatorDTO), SectionRequestDTO.class), locale);
    }

    @PostMapping(path = "/delete",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle sections delete request request ",notes = "Sections delete request success or failed")
    public ResponseEntity<ApiResponse<Object>> delete(@RequestBody @Validated(OnDelete.class) @Valid SectionRequestValidatorDTO sectionRequestValidatorDTO, Locale locale) {
        log.info("Sections delete request controller {} ", sectionRequestValidatorDTO);
        return sectionService.delete(gson.fromJson(gson.toJson(sectionRequestValidatorDTO), SectionRequestDTO.class), locale);
    }

}

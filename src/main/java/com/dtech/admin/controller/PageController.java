/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 12:28 PM
 * <p>
 */

package com.dtech.admin.controller;

import com.dtech.admin.dto.search.CommonSearchDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PageRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.PageRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.service.PageService;
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
@RequestMapping(path = "api/v1/page")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PageController {

    @Autowired
    private final PageService pageService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle page reference data find request request ",notes = "Page reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Page reference data request reference data controller {} ", channelRequestValidatorDTO);
        return pageService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle pages filter list request request ",notes = "Pages filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale) {
        log.info("Pages filter list request  controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<CommonSearchDTO>>(){}.getType();
        return pageService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle pages find by ID request request ",notes = "Pages find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid PageRequestValidatorDTO pageRequestValidatorDTO, Locale locale) {
        log.info("Pages find by ID request controller {} ", pageRequestValidatorDTO);
        return pageService.view(gson.fromJson(gson.toJson(pageRequestValidatorDTO), PageRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle pages update request request ",notes = "Pages update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid PageRequestValidatorDTO pageRequestValidatorDTO, Locale locale) {
        log.info("pages update request  controller {} ", pageRequestValidatorDTO);
        return pageService.update(gson.fromJson(gson.toJson(pageRequestValidatorDTO), PageRequestDTO.class), locale);
    }

    @PostMapping(path = "/delete",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle pages delete request request ",notes = "Pages delete request success or failed")
    public ResponseEntity<ApiResponse<Object>> delete(@RequestBody @Validated(OnDelete.class) @Valid PageRequestValidatorDTO pageRequestValidatorDTO, Locale locale) {
        log.info("Pages delete request  controller {} ", pageRequestValidatorDTO);
        return pageService.delete(gson.fromJson(gson.toJson(pageRequestValidatorDTO), PageRequestDTO.class), locale);
    }

}

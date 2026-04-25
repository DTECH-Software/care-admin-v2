package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.ReceivedClaimTotalReportSearchDTO;
import com.dtech.admin.service.ReceivedClaimTotalReportService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/reports/received-claim-total")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReceivedClaimTotalReportController {

    @Autowired
    private final ReceivedClaimTotalReportService reportService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle received claim total report reference data request", notes = "Received claim total report reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO,
                                                                Locale locale) {
        log.info("Received claim total report reference data request {}", validatorDTO);
        return reportService.getReferenceDate(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle received claim total report filter request", notes = "Received claim total report filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<ReceivedClaimTotalReportSearchDTO> paginationRequest,
                                                      Locale locale) {
        log.info("Received claim total report filter request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<ReceivedClaimTotalReportSearchDTO>>() {}.getType();
        return reportService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle received claim total report export request", notes = "Received claim total report export request success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Valid PaginationRequest<ReceivedClaimTotalReportSearchDTO> paginationRequest,
                                         Locale locale) {
        log.info("Received claim total report export request {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<ReceivedClaimTotalReportSearchDTO>>() {}.getType();
        return reportService.export(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }
}

package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.DailyTaskReportSearchDTO;
import com.dtech.admin.service.DailyTaskReportService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
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

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping(path = "api/v1/reports/daily-task")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DailyTaskReportController {

    @Autowired
    private final DailyTaskReportService reportService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final ObjectMapper objectMapper;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle daily task report reference data request", notes = "Daily task report reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO,
                                                                Locale locale) {
        log.info("Daily task report reference data request {}", validatorDTO);
        return reportService.getReferenceDate(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle daily task report filter request", notes = "Daily task report filter request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody Map<String, Object> requestBody,
                                                          Locale locale) {
        PaginationRequest<DailyTaskReportSearchDTO> paginationRequest = buildPaginationRequest(requestBody);
        log.info("Daily task report filter request {}", paginationRequest);
        return reportService.filterList(paginationRequest, locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle daily task report export request", notes = "Daily task report export request success or failed")
    public ResponseEntity<byte[]> export(@RequestBody Map<String, Object> requestBody,
                                         Locale locale) {
        PaginationRequest<DailyTaskReportSearchDTO> paginationRequest = buildPaginationRequest(requestBody);
        log.info("Daily task report export request {}", paginationRequest);
        return reportService.export(paginationRequest, locale);
    }

    private PaginationRequest<DailyTaskReportSearchDTO> buildPaginationRequest(Map<String, Object> requestBody) {
        PaginationRequest<DailyTaskReportSearchDTO> paginationRequest = objectMapper.convertValue(
                requestBody,
                new TypeReference<PaginationRequest<DailyTaskReportSearchDTO>>() {
                });

        Object filters = requestBody.get("search");
        if (filters == null) {
            filters = requestBody.get("filters");
        }
        if (filters == null) {
            filters = requestBody.get("filter");
        }

        DailyTaskReportSearchDTO search = filters != null
                ? objectMapper.convertValue(filters, DailyTaskReportSearchDTO.class)
                : paginationRequest.getSearch();
        if (search == null) {
            search = new DailyTaskReportSearchDTO();
        }

        applyRootString(requestBody, "claimType", search::setClaimType);
        applyRootString(requestBody, "companyCode", search::setCompanyCode);
        applyRootString(requestBody, "medicalOtherWorks", search::setMedicalOtherWorks);
        applyRootString(requestBody, "medicalOtherWork", search::setMedicalOtherWorks);
        applyRootString(requestBody, "ddfOtherWorks", search::setDdfOtherWorks);
        applyRootString(requestBody, "ddfOtherWork", search::setDdfOtherWorks);
        applyRootString(requestBody, "deathOtherWorks", search::setDdfOtherWorks);

        paginationRequest.setSearch(search);
        return paginationRequest;
    }

    private void applyRootString(Map<String, Object> requestBody,
                                 String key,
                                 java.util.function.Consumer<String> setter) {
        Object value = requestBody.get(key);
        if (value != null) {
            setter.accept(String.valueOf(value));
        }
    }
}

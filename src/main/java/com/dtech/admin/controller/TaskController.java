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
import com.dtech.admin.dto.request.TaskRequestDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.TaskRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.service.TaskService;
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
@RequestMapping(path = "api/v1/task")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private final TaskService taskService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle task reference data find request request ",notes = "Task reference data request success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Task reference data request reference data controller {} ", channelRequestValidatorDTO);
        return taskService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle tasks filter list request request ",notes = "Tasks filter list request success or failed")
    public ResponseEntity<ApiResponse<Object>> filterList(@RequestBody @Valid PaginationRequest<CommonSearchDTO> paginationRequest, Locale locale) {
        log.info("Tasks filter list request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<CommonSearchDTO>>(){}.getType();
        return taskService.filterList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle tasks find by ID request request ",notes = "Tasks find by ID request success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid TaskRequestValidatorDTO taskRequestValidatorDTO, Locale locale) {
        log.info("Tasks find by ID request controller {} ", taskRequestValidatorDTO);
        return taskService.view(gson.fromJson(gson.toJson(taskRequestValidatorDTO), TaskRequestDTO.class), locale);
    }

    @PostMapping(path = "/update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle tasks update request request ",notes = "Tasks update request success or failed")
    public ResponseEntity<ApiResponse<Object>> update(@RequestBody @Validated(OnUpdate.class) @Valid TaskRequestValidatorDTO taskRequestValidatorDTO, Locale locale) {
        log.info("Tasks update request controller {} ", taskRequestValidatorDTO);
        return taskService.update(gson.fromJson(gson.toJson(taskRequestValidatorDTO), TaskRequestDTO.class), locale);
    }

    @PostMapping(path = "/delete",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle tasks delete request request ",notes = "Tasks delete request success or failed")
    public ResponseEntity<ApiResponse<Object>> delete(@RequestBody @Validated(OnDelete.class) @Valid TaskRequestValidatorDTO taskRequestValidatorDTO, Locale locale) {
        log.info("Tasks delete request  controller {} ", taskRequestValidatorDTO);
        return taskService.delete(gson.fromJson(gson.toJson(taskRequestValidatorDTO), TaskRequestDTO.class), locale);
    }

}

package com.dtech.admin.controller;

import com.dtech.admin.dto.request.*;
import com.dtech.admin.dto.request.validator.PaymentAttachmentCreateValidatorDTO;
import com.dtech.admin.dto.request.validator.PaymentAttachmentIdValidatorDTO;
import com.dtech.admin.dto.request.validator.PaymentAttachmentRejectValidatorDTO;
import com.dtech.admin.dto.request.validator.PaymentAttachmentStatusUpdateValidatorDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.PaymentAttachmentClaimSearchDTO;
import com.dtech.admin.dto.search.PaymentAttachmentSearchDTO;
import com.dtech.admin.service.PaymentAttachmentService;
import com.dtech.admin.validator.OnAdd;
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
@RequestMapping(path = "api/v1/payment-attachments")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentAttachmentController {

    @Autowired
    private final PaymentAttachmentService paymentAttachmentService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment reference data request", notes = "Payment attachment reference data success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Payment attachment reference data {}", channelRequestValidatorDTO);
        return paymentAttachmentService.getReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/received/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle received payment attachment reference data request", notes = "Received payment attachment reference data success or failed")
    public ResponseEntity<ApiResponse<Object>> getReceivedReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Payment attachment received reference data {}", channelRequestValidatorDTO);
        return paymentAttachmentService.getReceivedReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/settled/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle settled payment attachment reference data request", notes = "Settled payment attachment reference data success or failed")
    public ResponseEntity<ApiResponse<Object>> getSettledReferenceDate(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Payment attachment settled reference data {}", channelRequestValidatorDTO);
        return paymentAttachmentService.getSettledReferenceDate(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/claims/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle eligible claim filter for payment attachment", notes = "Eligible claim filter list success or failed")
    public ResponseEntity<ApiResponse<Object>> filterEligibleClaims(@RequestBody @Valid PaginationRequest<PaymentAttachmentClaimSearchDTO> paginationRequest, Locale locale) {
        log.info("Payment attachment eligible claim filter list {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<PaymentAttachmentClaimSearchDTO>>() {}.getType();
        return paymentAttachmentService.filterEligibleClaims(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment filter list request", notes = "Payment attachment filter list success or failed")
    public ResponseEntity<ApiResponse<Object>> filterAttachments(@RequestBody @Valid PaginationRequest<PaymentAttachmentSearchDTO> paginationRequest, Locale locale) {
        log.info("Payment attachment filter list {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<PaymentAttachmentSearchDTO>>() {}.getType();
        return paymentAttachmentService.filterAttachments(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/received/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle received payment attachment filter list request", notes = "Received payment attachment filter list success or failed")
    public ResponseEntity<ApiResponse<Object>> filterReceivedAttachments(@RequestBody @Valid PaginationRequest<PaymentAttachmentSearchDTO> paginationRequest, Locale locale) {
        log.info("Payment attachment received filter list {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<PaymentAttachmentSearchDTO>>() {}.getType();
        return paymentAttachmentService.filterReceivedAttachments(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/status/update", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment status update request", notes = "Payment attachment status update success or failed")
    public ResponseEntity<ApiResponse<Object>> updateStatus(@RequestBody @Validated(OnUpdate.class) @Valid PaymentAttachmentStatusUpdateValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment attachment status update {}", validatorDTO);
        return paymentAttachmentService.updateStatus(gson.fromJson(gson.toJson(validatorDTO), PaymentAttachmentStatusUpdateDTO.class), locale);
    }

    @PostMapping(path = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment creation request", notes = "Payment attachment creation success or failed")
    public ResponseEntity<ApiResponse<Object>> create(@RequestBody @Validated(OnAdd.class) @Valid PaymentAttachmentCreateValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment attachment create {}", validatorDTO);
        return paymentAttachmentService.create(gson.fromJson(gson.toJson(validatorDTO), PaymentAttachmentCreateDTO.class), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment view request", notes = "Payment attachment view success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid PaymentAttachmentIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment attachment view {}", validatorDTO);
        return paymentAttachmentService.view(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }

    @PostMapping(path = "/finalize", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment finalize request", notes = "Payment attachment finalize success or failed")
    public ResponseEntity<ApiResponse<Object>> finalizeAttachment(@RequestBody @Validated(OnUpdate.class) @Valid PaymentAttachmentIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment attachment finalize {}", validatorDTO);
        PaymentAttachmentActionDTO dto = gson.fromJson(gson.toJson(validatorDTO), PaymentAttachmentActionDTO.class);
        dto.setId(validatorDTO.getId());
        return paymentAttachmentService.finalizeAttachment(dto, locale);
    }

    @PostMapping(path = "/reject", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment reject request", notes = "Payment attachment reject success or failed")
    public ResponseEntity<ApiResponse<Object>> reject(@RequestBody @Validated(OnUpdate.class) @Valid PaymentAttachmentRejectValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment attachment reject {}", validatorDTO);
        PaymentAttachmentActionDTO dto = gson.fromJson(gson.toJson(validatorDTO), PaymentAttachmentActionDTO.class);
        dto.setId(validatorDTO.getId());
        dto.setRemark(validatorDTO.getRemark());
        return paymentAttachmentService.rejectAttachment(dto, locale);
    }

    @PostMapping(path = "/print", produces = MediaType.TEXT_HTML_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment print request", notes = "Payment attachment print success or failed")
    public ResponseEntity<String> print(@RequestBody @Validated(OnGet.class) @Valid PaymentAttachmentIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment attachment print {}", validatorDTO);
        return paymentAttachmentService.print(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }

    @PostMapping(path = "/print/pdf", produces = MediaType.APPLICATION_PDF_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment PDF print request", notes = "Payment attachment PDF print success or failed")
    public ResponseEntity<byte[]> printPdf(@RequestBody @Validated(OnGet.class) @Valid PaymentAttachmentIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment attachment print pdf {}", validatorDTO);
        return paymentAttachmentService.printPdf(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }

    @PostMapping(path = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment attachment export request", notes = "Payment attachment export success or failed")
    public ResponseEntity<byte[]> export(@RequestBody @Validated(OnGet.class) @Valid PaymentAttachmentIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment attachment export {}", validatorDTO);
        return paymentAttachmentService.export(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }
}

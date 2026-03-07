package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.PaymentAdviceCreateDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.PaymentAdviceCreateValidatorDTO;
import com.dtech.admin.dto.request.validator.PaymentAdviceIdValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.PaymentAdviceAttachmentSearchDTO;
import com.dtech.admin.dto.search.PaymentAdviceSearchDTO;
import com.dtech.admin.service.PaymentAdviceService;
import com.dtech.admin.validator.OnAdd;
import com.dtech.admin.validator.OnGet;
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
@RequestMapping(path = "api/v1/payment-advice")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentAdviceController {

    @Autowired
    private final PaymentAdviceService paymentAdviceService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment advice reference data request", notes = "Payment advice reference data success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceData(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment advice reference data {}", validatorDTO);
        return paymentAdviceService.getReferenceData(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/attachments/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle eligible payment attachments filter request", notes = "Eligible payment attachments filter list success or failed")
    public ResponseEntity<ApiResponse<Object>> filterEligibleAttachments(@RequestBody @Valid PaginationRequest<PaymentAdviceAttachmentSearchDTO> paginationRequest, Locale locale) {
        log.info("Payment advice eligible attachments filter list {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<PaymentAdviceAttachmentSearchDTO>>() {}.getType();
        return paymentAdviceService.filterEligibleAttachments(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment advice creation request", notes = "Payment advice creation success or failed")
    public ResponseEntity<ApiResponse<Object>> create(@RequestBody @Validated(OnAdd.class) @Valid PaymentAdviceCreateValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment advice create {}", validatorDTO);
        return paymentAdviceService.create(gson.fromJson(gson.toJson(validatorDTO), PaymentAdviceCreateDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment advice filter list request", notes = "Payment advice filter list success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(@RequestBody @Valid PaginationRequest<PaymentAdviceSearchDTO> paginationRequest, Locale locale) {
        log.info("Payment advice filter list {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<PaymentAdviceSearchDTO>>() {}.getType();
        return paymentAdviceService.filter(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment advice view request", notes = "Payment advice view success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid PaymentAdviceIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment advice view {}", validatorDTO);
        return paymentAdviceService.view(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }

    @PostMapping(path = "/print", produces = MediaType.TEXT_HTML_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment advice print request", notes = "Payment advice print success or failed")
    public ResponseEntity<String> print(@RequestBody @Validated(OnGet.class) @Valid PaymentAdviceIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment advice print {}", validatorDTO);
        return paymentAdviceService.print(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }

    @PostMapping(path = "/print/pdf", produces = MediaType.APPLICATION_PDF_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle payment advice PDF print request", notes = "Payment advice PDF print success or failed")
    public ResponseEntity<byte[]> printPdf(@RequestBody @Validated(OnGet.class) @Valid PaymentAdviceIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Payment advice print pdf {}", validatorDTO);
        return paymentAdviceService.printPdf(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }
}

package com.dtech.admin.controller;

import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.PaymentAdviceDeathCreateDTO;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.request.validator.PaymentAdviceDeathCreateValidatorDTO;
import com.dtech.admin.dto.request.validator.PaymentAdviceIdValidatorDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.search.PaymentAdviceDeathClaimSearchDTO;
import com.dtech.admin.dto.search.PaymentAdviceDeathSearchDTO;
import com.dtech.admin.service.PaymentAdviceDeathService;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/payment-advice/death")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentAdviceDeathController {

    @Autowired
    private final PaymentAdviceDeathService paymentAdviceDeathService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death payment advice reference data request", notes = "Death payment advice reference data success or failed")
    public ResponseEntity<ApiResponse<Object>> getReferenceData(@RequestBody @Valid ChannelRequestValidatorDTO validatorDTO, Locale locale) {
        log.info("Death payment advice reference data {}", validatorDTO);
        return paymentAdviceDeathService.getReferenceData(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/claims/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle eligible death claims filter request", notes = "Eligible death claims filter list success or failed")
    public ResponseEntity<ApiResponse<Object>> filterEligibleClaims(
            @RequestBody @Valid PaginationRequest<PaymentAdviceDeathClaimSearchDTO> paginationRequest, Locale locale) {
        log.info("Death payment advice eligible claims filter list {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<PaymentAdviceDeathClaimSearchDTO>>() {}.getType();
        return paymentAdviceDeathService.filterEligibleClaims(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death payment advice creation request", notes = "Death payment advice creation success or failed")
    public ResponseEntity<ApiResponse<Object>> create(
            @RequestBody @Validated(OnAdd.class) @Valid PaymentAdviceDeathCreateValidatorDTO validatorDTO, Locale locale) {
        log.info("Death payment advice create {}", validatorDTO);
        return paymentAdviceDeathService.create(gson.fromJson(gson.toJson(validatorDTO), PaymentAdviceDeathCreateDTO.class), locale);
    }

    @PostMapping(path = "/filter", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death payment advice filter list request", notes = "Death payment advice filter list success or failed")
    public ResponseEntity<ApiResponse<Object>> filter(
            @RequestBody @Valid PaginationRequest<PaymentAdviceDeathSearchDTO> paginationRequest, Locale locale) {
        log.info("Death payment advice filter list {}", paginationRequest);
        Type paginationType = new TypeToken<PaginationRequest<PaymentAdviceDeathSearchDTO>>() {}.getType();
        return paymentAdviceDeathService.filter(gson.fromJson(gson.toJson(paginationRequest), paginationType), locale);
    }

    @PostMapping(path = "/view", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death payment advice view request", notes = "Death payment advice view success or failed")
    public ResponseEntity<ApiResponse<Object>> view(@RequestBody @Validated(OnGet.class) @Valid PaymentAdviceIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Death payment advice view {}", validatorDTO);
        return paymentAdviceDeathService.view(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }

    @PostMapping(path = "/print", produces = MediaType.TEXT_HTML_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death payment advice print request", notes = "Death payment advice print success or failed")
    public ResponseEntity<String> print(@RequestBody @Validated(OnGet.class) @Valid PaymentAdviceIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Death payment advice print {}", validatorDTO);
        return paymentAdviceDeathService.print(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }

    @PostMapping(path = "/print/pdf", produces = MediaType.APPLICATION_PDF_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death payment advice PDF print request", notes = "Death payment advice PDF print success or failed")
    public ResponseEntity<byte[]> printPdf(@RequestBody @Validated(OnGet.class) @Valid PaymentAdviceIdValidatorDTO validatorDTO, Locale locale) {
        log.info("Death payment advice print pdf {}", validatorDTO);
        return paymentAdviceDeathService.printPdf(gson.fromJson(gson.toJson(validatorDTO), ChannelRequestDTO.class), validatorDTO.getId(), locale);
    }
}

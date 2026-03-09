package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.PaymentAttachmentActionDTO;
import com.dtech.admin.dto.request.PaymentAttachmentCreateDTO;
import com.dtech.admin.dto.request.PaymentAttachmentStatusUpdateDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.PaymentAttachmentClaimResponseDTO;
import com.dtech.admin.dto.response.PaymentAttachmentListResponseDTO;
import com.dtech.admin.dto.response.PaymentAttachmentPrintSummaryDTO;
import com.dtech.admin.dto.response.PaymentAttachmentResponseDTO;
import com.dtech.admin.dto.search.PaymentAttachmentClaimSearchDTO;
import com.dtech.admin.dto.search.PaymentAttachmentSearchDTO;
import com.dtech.admin.dto.response.ClaimsRequestResponseDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.PaymentAttachmentClaimState;
import com.dtech.admin.enums.PaymentAttachmentStatus;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.entityToDto.ClaimsApprovalEntityToDto;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsDetails;
import com.dtech.admin.model.InsuranceDetailsLimit;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.model.PaymentAttachment;
import com.dtech.admin.model.PaymentAttachmentClaim;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.Treatment;
import com.dtech.admin.model.TreatmentCategory;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.repository.PaymentAttachmentClaimRepository;
import com.dtech.admin.repository.PaymentAttachmentRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.repository.TreatmentCategoryRepository;
import com.dtech.admin.repository.TreatmentRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.PaymentAttachmentService;
import com.dtech.admin.specifications.PaymentAttachmentClaimSpecification;
import com.dtech.admin.specifications.PaymentAttachmentSpecification;
import com.dtech.admin.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class PaymentAttachmentServiceImpl implements PaymentAttachmentService {

    private static final String DEFAULT_ATTACHMENT_PREFIX = "EX-OP1";
    private static final String STAFF_CATEGORY_MISMATCH = "__STAFF_CATEGORY_MISMATCH__";
    private static final int POLICY_YEAR_MISMATCH = -1;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final PaymentAttachmentRepository paymentAttachmentRepository;

    @Autowired
    private final PaymentAttachmentClaimRepository paymentAttachmentClaimRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final TreatmentCategoryRepository treatmentCategoryRepository;

    @Autowired
    private final TreatmentRepository treatmentRepository;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final ClaimsApprovalEntityToDto claimsApprovalEntityToDto;

    @Autowired
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Payment attachment reference data {}", channelRequestDTO);
            return buildReferenceData(channelRequestDTO, WebPage.PAMC, locale);
        } catch (Exception e) {
            log.error("Failed to load payment attachment reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReceivedReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Payment attachment received reference data {}", channelRequestDTO);
            return buildReferenceData(channelRequestDTO, WebPage.PARE, locale);
        } catch (Exception e) {
            log.error("Failed to load payment attachment received reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getSettledReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Payment attachment settled reference data {}", channelRequestDTO);
            return buildReferenceData(channelRequestDTO, WebPage.PASE, locale);
        } catch (Exception e) {
            log.error("Failed to load payment attachment settled reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterEligibleClaims(PaginationRequest<PaymentAttachmentClaimSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Payment attachment eligible claim filter {}", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            PaymentAttachmentClaimSearchDTO filter = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(PaymentAttachmentClaimSearchDTO::new);

            Page<InsuranceClaimsRequest> claims = insuranceClaimsRequestRepository
                    .findAll(PaymentAttachmentClaimSpecification.getSpecification(filter), pageable);

            long total = insuranceClaimsRequestRepository.count(PaymentAttachmentClaimSpecification.getSpecification(filter));

            List<InsuranceClaimsRequest> claimList = claims.getContent();
            Set<Long> activeClaimIds = claimList.isEmpty()
                    ? Collections.emptySet()
                    : paymentAttachmentClaimRepository.findAllByInsuranceClaimsRequestInAndState(claimList, PaymentAttachmentClaimState.ACTIVE).stream()
                            .map(PaymentAttachmentClaim::getInsuranceClaimsRequest)
                            .filter(Objects::nonNull)
                            .map(InsuranceClaimsRequest::getId)
                            .collect(Collectors.toSet());

            Set<Long> releasedClaimIds = claimList.isEmpty()
                    ? Collections.emptySet()
                    : paymentAttachmentClaimRepository.findAllByInsuranceClaimsRequestInAndState(claimList, PaymentAttachmentClaimState.RELEASED).stream()
                            .map(PaymentAttachmentClaim::getInsuranceClaimsRequest)
                            .filter(Objects::nonNull)
                            .map(InsuranceClaimsRequest::getId)
                            .collect(Collectors.toSet());

            List<ClaimsRequestResponseDTO> responseDTOList = claimList.stream()
                    .map(claim -> claimsApprovalEntityToDto.mapClaimsApproval(claim, false))
                    .collect(Collectors.toList());

            List<Map<String, Object>> sanitizedResponse = responseDTOList.stream()
                    .map(dto -> sanitizeClaimResponse(dto, activeClaimIds, releasedClaimIds))
                    .collect(Collectors.toList());

            auditLogService.log(WebPage.PAMC.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(),
                    paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(sanitizedResponse), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<>(sanitizedResponse, sanitizedResponse.size(), total),
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_CLAIM_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter eligible claims", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> create(PaymentAttachmentCreateDTO paymentAttachmentCreateDTO, Locale locale) {
        try {
            log.info("Creating payment attachment {}", paymentAttachmentCreateDTO);

            List<InsuranceClaimsRequest> claims = insuranceClaimsRequestRepository.findAllById(paymentAttachmentCreateDTO.getClaimIds());
            if (claims.isEmpty()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 404,
                        messageSource.getMessage(ResponseMessageUtil.CLAIMS_REQUEST_NOT_FOUND, null, locale)));
            }

            String derivedStaffCategory = resolveStaffCategoryCode(claims);
            if (STAFF_CATEGORY_MISMATCH.equals(derivedStaffCategory)) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1050,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_STAFF_CATEGORY_MISMATCH, null, locale)));
            }

            if (!hasText(derivedStaffCategory)) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1051,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_STAFF_CATEGORY_MISSING, null, locale)));
            }

            if (hasText(paymentAttachmentCreateDTO.getStaffCategoryCode())
                    && !derivedStaffCategory.equalsIgnoreCase(paymentAttachmentCreateDTO.getStaffCategoryCode())) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1050,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_STAFF_CATEGORY_MISMATCH, null, locale)));
            }

            if (!hasText(paymentAttachmentCreateDTO.getStaffCategoryCode())) {
                paymentAttachmentCreateDTO.setStaffCategoryCode(derivedStaffCategory);
            }

            String paymentCompanyCode = resolvePaymentCompanyCodeFromRequests(claims);
            String derivedCompanyCode = hasText(paymentCompanyCode)
                    ? paymentCompanyCode
                    : resolveCompanyCode(claims);
            if (hasText(derivedCompanyCode)) {
                paymentAttachmentCreateDTO.setCompanyCode(derivedCompanyCode);
            }

            if (!hasText(paymentAttachmentCreateDTO.getAttachmentPrefix())) {
                paymentAttachmentCreateDTO.setAttachmentPrefix(paymentAttachmentCreateDTO.getStaffCategoryCode());
            }

            Integer policyYear = resolveAttachmentPolicyYear(claims);
            if (policyYear == null || policyYear == POLICY_YEAR_MISMATCH) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1052,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_POLICY_YEAR_MISMATCH, null, locale)));
            }

            Optional<InsuranceClaimsRequest> alreadyAttached = claims.stream()
                    .filter(claim -> paymentAttachmentClaimRepository.existsByInsuranceClaimsRequestAndState(claim, PaymentAttachmentClaimState.ACTIVE))
                    .findFirst();

            if (alreadyAttached.isPresent()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1046,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_CLAIM_ALREADY_IN_ATTACHMENT,
                                new Object[]{alreadyAttached.get().getRequestId()}, locale)));
            }

            PaymentAttachment attachment = buildAttachment(paymentAttachmentCreateDTO, claims);

            List<PaymentAttachmentClaim> attachmentClaims = claims.stream()
                    .map(claim -> buildAttachmentClaim(attachment, claim))
                    .collect(Collectors.toList());

            attachment.setClaims(attachmentClaims);
            paymentAttachmentRepository.saveAndFlush(attachment);

            PaymentAttachmentResponseDTO responseDTO = mapAttachmentToResponse(attachment, true);

            auditLogService.log(WebPage.PAMC.name(), WebTask.ADD.name(), AuditTask.ADD_DATA.getDescription(),
                    paymentAttachmentCreateDTO.getIp(), paymentAttachmentCreateDTO.getUserAgent(), gson.toJson(responseDTO), null, paymentAttachmentCreateDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_CREATED_SUCCESS,
                            new Object[]{attachmentClaims.size(), attachment.getAttachmentNo()}, locale)));
        } catch (Exception e) {
            log.error("Failed to create payment attachment", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("View payment attachment {}", id);
            return paymentAttachmentRepository.findById(id)
                    .map(attachment -> {
                        PaymentAttachmentResponseDTO responseDTO = mapAttachmentToResponse(attachment, true);
                    WebPage page = resolveAttachmentPage(attachment);
                    auditLogService.log(page.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                            requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(responseDTO), null, requestDTO.getUsername());
                    return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                            messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_RETRIEVE_SUCCESSFULLY, null, locale)));
                    })
                    .orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 1047,
                            messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_NOT_FOUND, new Object[]{id}, locale))));
        } catch (Exception e) {
            log.error("Failed to view payment attachment {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterAttachments(PaginationRequest<PaymentAttachmentSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Filter payment attachments {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            PaymentAttachmentSearchDTO filter = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(PaymentAttachmentSearchDTO::new);

            Page<PaymentAttachment> attachments = paymentAttachmentRepository.findAll(PaymentAttachmentSpecification.getSpecification(filter), pageable);
            long total = paymentAttachmentRepository.count(PaymentAttachmentSpecification.getSpecification(filter));

            Map<String, String> companyDescriptions = loadCompanyDescriptions();
            Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();
            Map<String, String> treatmentCategoryDescriptions = loadTreatmentCategoryDescriptions();
            Map<String, String> treatmentDescriptions = loadTreatmentDescriptions();
            List<PaymentAttachmentListResponseDTO> response = attachments.stream()
                    .map(attachment -> mapAttachmentToListResponse(attachment, companyDescriptions,
                            staffCategoryDescriptions, treatmentCategoryDescriptions, treatmentDescriptions))
                    .collect(Collectors.toList());

            WebPage page = resolveAttachmentListPage(filter);
            auditLogService.log(page.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(),
                    paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(response), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<>(response, response.size(), total),
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter payment attachments", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterReceivedAttachments(PaginationRequest<PaymentAttachmentSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Filter received payment attachments {}", paginationRequest);
            PaymentAttachmentSearchDTO filter = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(PaymentAttachmentSearchDTO::new);

            Set<String> allowedStatuses = Set.of(PaymentAttachmentStatus.DRAFT.name(), PaymentAttachmentStatus.REJECTED.name());
            List<String> filteredStatuses = Optional.ofNullable(filter.getStatus()).orElse(List.of()).stream()
                    .filter(this::hasText)
                    .map(status -> status.trim().toUpperCase())
                    .filter(allowedStatuses::contains)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));

            if (filteredStatuses.isEmpty()) {
                filteredStatuses = new ArrayList<>(allowedStatuses);
            }

            filter.setStatus(filteredStatuses);
            paginationRequest.setSearch(filter);
            return filterAttachments(paginationRequest, locale);
        } catch (Exception e) {
            log.error("Failed to filter received payment attachments", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> updateStatus(PaymentAttachmentStatusUpdateDTO statusUpdateDTO, Locale locale) {
        try {
            log.info("Update payment attachment status {}", statusUpdateDTO);

            PaymentAttachmentStatus status = PaymentAttachmentStatus.valueOf(statusUpdateDTO.getStatus().trim().toUpperCase());
            PaymentAttachmentActionDTO actionDTO = toActionDTO(statusUpdateDTO);

            if (PaymentAttachmentStatus.FINALIZED.equals(status)) {
                return finalizeAttachment(actionDTO, locale);
            }

            if (PaymentAttachmentStatus.REJECTED.equals(status)) {
                return rejectAttachment(actionDTO, locale);
            }

            return paymentAttachmentRepository.findById(statusUpdateDTO.getId())
                    .map(attachment -> ResponseEntity.ok().body(responseUtil.error(null, 1048,
                            messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_STATUS_NOT_CHANGING,
                                    new Object[]{attachment.getAttachmentNo()}, locale))))
                    .orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 1047,
                            messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_NOT_FOUND,
                                    new Object[]{statusUpdateDTO.getId()}, locale))));
        } catch (IllegalArgumentException e) {
            log.error("Invalid payment attachment status {}", statusUpdateDTO.getStatus(), e);
            return ResponseEntity.ok().body(responseUtil.error(null, 1048,
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_STATUS_NOT_CHANGING,
                            new Object[]{statusUpdateDTO.getStatus()}, locale)));
        } catch (Exception e) {
            log.error("Failed to update payment attachment status {}", statusUpdateDTO.getId(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> finalizeAttachment(PaymentAttachmentActionDTO actionDTO, Locale locale) {
        try {
            log.info("Finalize payment attachment {}", actionDTO.getId());
            return paymentAttachmentRepository.findById(actionDTO.getId())
                    .map(attachment -> {
                        if (PaymentAttachmentStatus.REJECTED.equals(attachment.getStatus()) || PaymentAttachmentStatus.FINALIZED.equals(attachment.getStatus())) {
                            return ResponseEntity.ok().body(responseUtil.error(null, 1048,
                                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_STATUS_NOT_CHANGING, new Object[]{attachment.getAttachmentNo()}, locale)));
                        }
                        attachment.setStatus(PaymentAttachmentStatus.FINALIZED);
                        paymentAttachmentRepository.saveAndFlush(attachment);

                        PaymentAttachmentResponseDTO responseDTO = mapAttachmentToResponse(attachment, true);
                        auditLogService.log(WebPage.PARE.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(),
                                actionDTO.getIp(), actionDTO.getUserAgent(), gson.toJson(responseDTO), null, actionDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                                messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_FINALIZED_SUCCESS, new Object[]{attachment.getAttachmentNo()}, locale)));
                    })
                    .orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 1047,
                            messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_NOT_FOUND, new Object[]{actionDTO.getId()}, locale))));
        } catch (Exception e) {
            log.error("Failed to finalize payment attachment {}", actionDTO.getId(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> rejectAttachment(PaymentAttachmentActionDTO actionDTO, Locale locale) {
        try {
            log.info("Reject payment attachment {}", actionDTO.getId());
            return paymentAttachmentRepository.findById(actionDTO.getId())
                    .map(attachment -> {
                        if (PaymentAttachmentStatus.REJECTED.equals(attachment.getStatus())) {
                            return ResponseEntity.ok().body(responseUtil.error(null, 1048,
                                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_STATUS_NOT_CHANGING, new Object[]{attachment.getAttachmentNo()}, locale)));
                        }

                        attachment.setStatus(PaymentAttachmentStatus.REJECTED);
                        if (hasText(actionDTO.getRemark())) {
                            attachment.setNotes(actionDTO.getRemark());
                        }

                        List<PaymentAttachmentClaim> claims = paymentAttachmentClaimRepository.findAllByPaymentAttachment(attachment);
                        claims.forEach(claim -> claim.setState(PaymentAttachmentClaimState.RELEASED));

                        paymentAttachmentRepository.saveAndFlush(attachment);

                        PaymentAttachmentResponseDTO responseDTO = mapAttachmentToResponse(attachment, true);
                        auditLogService.log(WebPage.PARE.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(),
                                actionDTO.getIp(), actionDTO.getUserAgent(), gson.toJson(responseDTO), null, actionDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                                messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_REJECTED_SUCCESS, new Object[]{attachment.getAttachmentNo()}, locale)));
                    })
                    .orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 1047,
                            messageSource.getMessage(ResponseMessageUtil.PAYMENT_ATTACHMENT_NOT_FOUND, new Object[]{actionDTO.getId()}, locale))));
        } catch (Exception e) {
            log.error("Failed to reject payment attachment {}", actionDTO.getId(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<String> print(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("Print payment attachment {}", id);
            return paymentAttachmentRepository.findById(id)
                    .map(attachment -> {
                        PaymentAttachmentResponseDTO responseDTO = mapAttachmentToResponse(attachment, true);
                        PaymentAttachmentPrintSummaryDTO summary = buildPrintSummary(responseDTO);
                        String html = buildPrintHtml(responseDTO, summary);

                        WebPage page = resolveAttachmentPage(attachment);
                        auditLogService.log(page.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                                requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(summary), null, requestDTO.getUsername());

                        return ResponseEntity.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .body(html);
                    })
                    .orElseGet(() -> ResponseEntity.status(404).body("Payment attachment not found: " + id));
        } catch (Exception e) {
            log.error("Failed to print payment attachment {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> printPdf(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("Print PDF payment attachment {}", id);
            return paymentAttachmentRepository.findById(id)
                    .map(attachment -> {
                        PaymentAttachmentResponseDTO responseDTO = mapAttachmentToResponse(attachment, true);
                        PaymentAttachmentPrintSummaryDTO summary = buildPrintSummary(responseDTO);
                        byte[] pdfBytes = buildPdf(responseDTO, summary);

                        WebPage page = resolveAttachmentPage(attachment);
                        auditLogService.log(page.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                                requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(summary), null, requestDTO.getUsername());

                        String fileName = "payment-attachment-" + safeFileName(responseDTO.getAttachmentNo()) + ".pdf";
                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                                .body(pdfBytes);
                    })
                    .orElseGet(() -> ResponseEntity.status(404)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(("Payment attachment not found: " + id).getBytes()));
        } catch (Exception e) {
            log.error("Failed to print payment attachment PDF {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("Export payment attachment {}", id);
            return paymentAttachmentRepository.findById(id)
                    .map(attachment -> {
                        PaymentAttachmentResponseDTO responseDTO = mapAttachmentToResponse(attachment, true);
                        PaymentAttachmentPrintSummaryDTO summary = buildPrintSummary(responseDTO);
                        byte[] excelBytes = buildExcel(responseDTO, summary);

                        WebPage page = resolveAttachmentPage(attachment);
                        auditLogService.log(page.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                                requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(summary), null, requestDTO.getUsername());

                        String fileName = "payment-attachment-" + safeFileName(responseDTO.getAttachmentNo()) + ".xlsx";
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                                .body(excelBytes);
                    })
                    .orElseGet(() -> ResponseEntity.status(404)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(("Payment attachment not found: " + id).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Failed to export payment attachment {}", id, e);
            throw e;
        }
    }

    private PaymentAttachment buildAttachment(PaymentAttachmentCreateDTO dto, List<InsuranceClaimsRequest> claims) {
        PaymentAttachment attachment = new PaymentAttachment();
        String prefix = hasText(dto.getAttachmentPrefix()) ? dto.getAttachmentPrefix() : DEFAULT_ATTACHMENT_PREFIX;
        int year = resolveAttachmentYear(dto, claims);

        Integer nextSequence = paymentAttachmentRepository
                .findTopByAttachmentPrefixAndAttachmentYearOrderByAttachmentSequenceDesc(prefix, year)
                .map(val -> val.getAttachmentSequence() + 1)
                .orElse(1);

        String attachmentNo = prefix + "/" + year + "/" + String.format("%04d", nextSequence);

        attachment.setAttachmentNo(attachmentNo);
        attachment.setAttachmentPrefix(prefix);
        attachment.setAttachmentYear(year);
        attachment.setAttachmentSequence(nextSequence);
        attachment.setNotes(dto.getNotes());
        attachment.setCompanyCode(dto.getCompanyCode());
        attachment.setStaffCategoryCode(dto.getStaffCategoryCode());
        attachment.setTreatmentCategory(dto.getTreatmentCategory());
        attachment.setDateFrom(parseDate(dto.getDateFrom(), true));
        attachment.setDateTo(parseDate(dto.getDateTo(), false));
        attachment.setStatus(PaymentAttachmentStatus.DRAFT);
        return attachment;
    }

    private int resolveAttachmentYear(PaymentAttachmentCreateDTO dto, List<InsuranceClaimsRequest> claims) {
        Integer policyYear = resolveAttachmentPolicyYear(claims);
        if (policyYear != null && policyYear != POLICY_YEAR_MISMATCH) {
            return policyYear;
        }

        Date dateFrom = parseDate(dto.getDateFrom(), true);
        if (dateFrom != null) {
            return DateTimeUtil.getYear(dateFrom);
        }
        Date dateTo = parseDate(dto.getDateTo(), false);
        if (dateTo != null) {
            return DateTimeUtil.getYear(dateTo);
        }

        return LocalDate.now().getYear();
    }

    private InsuranceStaffCategoryPeriod resolveAttachmentPolicyPeriod(InsuranceClaimsRequest claim) {
        InsuranceDetailsLimit detailsLimit = claim != null ? claim.getInsuranceDetailsLimit() : null;
        InsuranceStaffCategoryPeriod approvalPeriod = detailsLimit != null ? detailsLimit.getInsuranceStaffCategoryPeriod() : null;
        if (approvalPeriod != null) {
            return approvalPeriod;
        }

        InsuranceClaimsDetails details = claim != null ? claim.getInsuranceClaimsDetails() : null;
        return details != null ? details.getInsuranceStaffCategoryPeriod() : null;
    }

    private Integer resolveAttachmentPolicyYear(List<InsuranceClaimsRequest> claims) {
        Integer policyYear = null;
        if (claims == null) {
            return null;
        }

        for (InsuranceClaimsRequest claim : claims) {
            InsuranceStaffCategoryPeriod period = resolveAttachmentPolicyPeriod(claim);
            Date fromDate = period != null ? period.getFromDate() : null;
            Date toDate = period != null ? period.getToDate() : null;
            Integer currentYear = fromDate != null ? DateTimeUtil.getYear(fromDate)
                    : (toDate != null ? DateTimeUtil.getYear(toDate) : null);
            if (currentYear == null) {
                return null;
            }
            if (policyYear == null) {
                policyYear = currentYear;
            } else if (!policyYear.equals(currentYear)) {
                return POLICY_YEAR_MISMATCH;
            }
        }
        return policyYear;
    }

    private PaymentAttachmentClaim buildAttachmentClaim(PaymentAttachment attachment, InsuranceClaimsRequest claim) {
        PaymentAttachmentClaim attachmentClaim = new PaymentAttachmentClaim();
        attachmentClaim.setPaymentAttachment(attachment);
        attachmentClaim.setInsuranceClaimsRequest(claim);
        attachmentClaim.setState(PaymentAttachmentClaimState.ACTIVE);
        attachmentClaim.setRequestId(claim.getRequestId());

        UserPersonalDetails personalDetails = claim.getEmployee().getUserPersonalDetails();
        attachmentClaim.setEmployeeName(buildEmployeeName(personalDetails));
        attachmentClaim.setEpf(personalDetails.getEpfNo());
        if (personalDetails.getUserCompanyDetails() != null) {
            attachmentClaim.setCompanyCode(personalDetails.getUserCompanyDetails().getCompanyTypes().getCode());
            attachmentClaim.setStaffCategoryCode(personalDetails.getUserCompanyDetails().getStaffCategories().getCode());
        }

        InsuranceClaimsDetails details = claim.getInsuranceClaimsDetails();
        if (details != null) {
            if (details.getTreatmentCategory() != null) {
                attachmentClaim.setTreatmentCategory(details.getTreatmentCategory().getCode());
            }
            if (details.getTreatment() != null) {
                attachmentClaim.setClaimCategory(details.getTreatment().getTreatmentCode());
            }
        }

        attachmentClaim.setRequestAmount(claim.getRequestAmount());
        attachmentClaim.setApprovedAmount(Optional.ofNullable(claim.getApprovedAmount()).orElse(BigDecimal.ZERO));
        attachmentClaim.setClaimStatus(claim.getRequestStatus());
        attachmentClaim.setRemark(resolveApprovalRemark(claim));
        return attachmentClaim;
    }

    private PaymentAttachmentResponseDTO mapAttachmentToResponse(PaymentAttachment attachment, boolean includeClaims) {
        PaymentAttachmentResponseDTO dto = new PaymentAttachmentResponseDTO();
        Map<String, String> companyDescriptions = loadCompanyDescriptions();
        Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();
        Map<String, String> treatmentCategoryDescriptions = loadTreatmentCategoryDescriptions();
        Map<String, String> treatmentDescriptions = loadTreatmentDescriptions();
        dto.setId(attachment.getId());
        dto.setAttachmentNo(attachment.getAttachmentNo());
        dto.setAttachmentPrefix(attachment.getAttachmentPrefix());
        dto.setAttachmentYear(attachment.getAttachmentYear());
        dto.setAttachmentSequence(attachment.getAttachmentSequence());
        dto.setNotes(attachment.getNotes());
        dto.setCompanyCode(attachment.getCompanyCode());
        dto.setCompanyDescription(resolveDescription(companyDescriptions, attachment.getCompanyCode()));
        String paymentCompanyCode = resolvePaymentCompanyCodeFromAttachmentClaims(
                attachment.getClaims().isEmpty()
                        ? paymentAttachmentClaimRepository.findAllByPaymentAttachment(attachment)
                        : attachment.getClaims());
        dto.setPaymentCompanyCode(paymentCompanyCode);
        dto.setPaymentCompanyDescription(resolveDescription(companyDescriptions, paymentCompanyCode));
        dto.setStaffCategoryCode(attachment.getStaffCategoryCode());
        dto.setStaffCategoryDescription(resolveDescription(staffCategoryDescriptions, attachment.getStaffCategoryCode()));
        dto.setTreatmentCategory(attachment.getTreatmentCategory());
        dto.setTreatmentCategoryDescription(resolveTreatmentDescription(attachment.getTreatmentCategory(),
                treatmentCategoryDescriptions, treatmentDescriptions));
        dto.setDateFrom(attachment.getDateFrom());
        dto.setDateTo(attachment.getDateTo());
        dto.setStatus(attachment.getStatus().name());
        dto.setCreatedDate(attachment.getCreatedDate());
        dto.setCreatedBy(attachment.getCreatedBy());
        dto.setLastModifiedDate(attachment.getLastModifiedDate());
        dto.setLastModifiedBy(attachment.getLastModifiedBy());

        if (includeClaims) {
            List<PaymentAttachmentClaim> claims = attachment.getClaims().isEmpty() ?
                    paymentAttachmentClaimRepository.findAllByPaymentAttachment(attachment) :
                    attachment.getClaims();
            dto.setClaims(claims.stream()
                    .map(claim -> mapClaimToResponse(claim, companyDescriptions, staffCategoryDescriptions,
                            treatmentCategoryDescriptions, treatmentDescriptions))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private PaymentAttachmentClaimResponseDTO mapClaimToResponse(PaymentAttachmentClaim claim,
                                                                Map<String, String> companyDescriptions,
                                                                Map<String, String> staffCategoryDescriptions,
                                                                Map<String, String> treatmentCategoryDescriptions,
                                                                Map<String, String> treatmentDescriptions) {
        PaymentAttachmentClaimResponseDTO dto = new PaymentAttachmentClaimResponseDTO();
        dto.setId(claim.getId());
        dto.setClaimId(claim.getInsuranceClaimsRequest().getId());
        dto.setRequestId(claim.getRequestId());
        dto.setEmployeeName(claim.getEmployeeName());
        dto.setEpf(claim.getEpf());
        dto.setCompanyCode(claim.getCompanyCode());
        dto.setCompanyDescription(resolveDescription(companyDescriptions, claim.getCompanyCode()));
        String paymentCompanyCode = Optional.ofNullable(claim.getInsuranceClaimsRequest())
                .map(InsuranceClaimsRequest::getEmployee)
                .map(ApplicationUser::getUserPersonalDetails)
                .map(UserPersonalDetails::getUserCompanyDetails)
                .map(UserCompanyDetails::getPaymentCompany)
                .map(CompanyTypes::getCode)
                .orElse(null);
        dto.setPaymentCompanyCode(paymentCompanyCode);
        dto.setPaymentCompanyDescription(resolveDescription(companyDescriptions, paymentCompanyCode));
        dto.setStaffCategoryCode(claim.getStaffCategoryCode());
        dto.setStaffCategoryDescription(resolveDescription(staffCategoryDescriptions, claim.getStaffCategoryCode()));
        dto.setTreatmentCategory(claim.getTreatmentCategory());
        dto.setTreatmentCategoryDescription(resolveTreatmentDescription(claim.getTreatmentCategory(),
                treatmentCategoryDescriptions, treatmentDescriptions));
        dto.setClaimCategory(claim.getClaimCategory());
        dto.setClaimCategoryDescription(resolveDescription(treatmentDescriptions, claim.getClaimCategory()));
        dto.setRequestAmount(claim.getRequestAmount());
        dto.setApprovedAmount(claim.getApprovedAmount());
        dto.setClaimStatus(claim.getClaimStatus() != null ? claim.getClaimStatus().name() : null);
        dto.setRemark(resolveApprovalRemark(claim.getInsuranceClaimsRequest()));
        return dto;
    }

    private PaymentAttachmentListResponseDTO mapAttachmentToListResponse(PaymentAttachment attachment,
                                                                        Map<String, String> companyDescriptions,
                                                                        Map<String, String> staffCategoryDescriptions,
                                                                        Map<String, String> treatmentCategoryDescriptions,
                                                                        Map<String, String> treatmentDescriptions) {
        PaymentAttachmentListResponseDTO dto = new PaymentAttachmentListResponseDTO();
        dto.setId(attachment.getId());
        dto.setAttachmentNo(attachment.getAttachmentNo());
        dto.setStatus(attachment.getStatus().name());
        dto.setCompanyCode(attachment.getCompanyCode());
        dto.setCompanyDescription(resolveDescription(companyDescriptions, attachment.getCompanyCode()));
        String paymentCompanyCode = resolvePaymentCompanyCodeFromAttachmentClaims(
                paymentAttachmentClaimRepository.findAllByPaymentAttachment(attachment));
        dto.setPaymentCompanyCode(paymentCompanyCode);
        dto.setPaymentCompanyDescription(resolveDescription(companyDescriptions, paymentCompanyCode));
        dto.setStaffCategoryCode(attachment.getStaffCategoryCode());
        dto.setStaffCategoryDescription(resolveDescription(staffCategoryDescriptions, attachment.getStaffCategoryCode()));
        dto.setTreatmentCategory(attachment.getTreatmentCategory());
        dto.setTreatmentCategoryDescription(resolveTreatmentDescription(attachment.getTreatmentCategory(),
                treatmentCategoryDescriptions, treatmentDescriptions));
        dto.setDateFrom(attachment.getDateFrom());
        dto.setDateTo(attachment.getDateTo());
        dto.setCreatedDate(attachment.getCreatedDate());
        dto.setCreatedBy(attachment.getCreatedBy());
        return dto;
    }

    private PaymentAttachmentActionDTO toActionDTO(PaymentAttachmentStatusUpdateDTO statusUpdateDTO) {
        PaymentAttachmentActionDTO actionDTO = new PaymentAttachmentActionDTO();
        actionDTO.setId(statusUpdateDTO.getId());
        actionDTO.setRemark(statusUpdateDTO.getRemark());
        actionDTO.setIp(statusUpdateDTO.getIp());
        actionDTO.setMessage(statusUpdateDTO.getMessage());
        actionDTO.setUsername(statusUpdateDTO.getUsername());
        actionDTO.setUserAgent(statusUpdateDTO.getUserAgent());
        return actionDTO;
    }

    private String resolveStaffCategoryCode(List<InsuranceClaimsRequest> claims) {
        String staffCategory = null;
        for (InsuranceClaimsRequest claim : claims) {
            String claimStaffCategory = Optional.ofNullable(claim.getEmployee())
                    .map(ApplicationUser::getUserPersonalDetails)
                    .map(UserPersonalDetails::getUserCompanyDetails)
                    .map(UserCompanyDetails::getStaffCategories)
                    .map(StaffCategories::getCode)
                    .orElse(null);

            if (!hasText(claimStaffCategory)) {
                return null;
            }

            if (staffCategory == null) {
                staffCategory = claimStaffCategory;
            } else if (!staffCategory.equalsIgnoreCase(claimStaffCategory)) {
                return STAFF_CATEGORY_MISMATCH;
            }
        }
        return staffCategory;
    }

    private String resolveApprovalRemark(InsuranceClaimsRequest claim) {
        if (claim == null || claim.getApprovalWorkFlows() == null) {
            return null;
        }
        return claim.getApprovalWorkFlows().stream()
                .filter(workflow -> hasText(workflow.getRejectedRemark()))
                .max(Comparator.comparing(ApprovalWorkFlow::getApprovedDate, Comparator.nullsLast(Date::compareTo)))
                .map(ApprovalWorkFlow::getRejectedRemark)
                .orElse(null);
    }

    private String resolvePaymentCompanyCodeFromRequests(List<InsuranceClaimsRequest> claims) {
        String paymentCompany = null;
        for (InsuranceClaimsRequest claim : claims) {
            String claimPaymentCompany = Optional.ofNullable(claim.getEmployee())
                    .map(ApplicationUser::getUserPersonalDetails)
                    .map(UserPersonalDetails::getUserCompanyDetails)
                    .map(UserCompanyDetails::getPaymentCompany)
                    .map(CompanyTypes::getCode)
                    .orElse(null);

            if (!hasText(claimPaymentCompany)) {
                continue;
            }

            if (paymentCompany == null) {
                paymentCompany = claimPaymentCompany;
            } else if (!paymentCompany.equalsIgnoreCase(claimPaymentCompany)) {
                return null;
            }
        }
        return paymentCompany;
    }

    private String resolveCompanyCode(List<InsuranceClaimsRequest> claims) {
        String company = null;
        for (InsuranceClaimsRequest claim : claims) {
            String claimCompany = Optional.ofNullable(claim.getEmployee())
                    .map(ApplicationUser::getUserPersonalDetails)
                    .map(UserPersonalDetails::getUserCompanyDetails)
                    .map(UserCompanyDetails::getCompanyTypes)
                    .map(CompanyTypes::getCode)
                    .orElse(null);

            if (!hasText(claimCompany)) {
                continue;
            }

            if (company == null) {
                company = claimCompany;
            } else if (!company.equalsIgnoreCase(claimCompany)) {
                return null;
            }
        }
        return company;
    }

    private String resolvePaymentCompanyCodeFromAttachmentClaims(List<PaymentAttachmentClaim> claims) {
        String paymentCompany = null;
        for (PaymentAttachmentClaim claim : claims) {
            String claimPaymentCompany = Optional.ofNullable(claim.getInsuranceClaimsRequest())
                    .map(InsuranceClaimsRequest::getEmployee)
                    .map(ApplicationUser::getUserPersonalDetails)
                    .map(UserPersonalDetails::getUserCompanyDetails)
                    .map(UserCompanyDetails::getPaymentCompany)
                    .map(CompanyTypes::getCode)
                    .orElse(null);

            if (!hasText(claimPaymentCompany)) {
                continue;
            }

            if (paymentCompany == null) {
                paymentCompany = claimPaymentCompany;
            } else if (!paymentCompany.equalsIgnoreCase(claimPaymentCompany)) {
                return null;
            }
        }
        return paymentCompany;
    }

    private Map<String, Object> sanitizeClaimResponse(ClaimsRequestResponseDTO claimsRequestResponseDTO,
                                                      Set<Long> activeClaimIds,
                                                      Set<Long> releasedClaimIds) {
        Map<String, Object> dtoMap = objectMapper.convertValue(claimsRequestResponseDTO, new TypeReference<Map<String, Object>>() {});

        Object insuranceDetailsObj = dtoMap.get("insuranceClaimsDetails");
        if (insuranceDetailsObj instanceof Map<?, ?> insuranceDetailsMap) {
            ((Map<String, Object>) insuranceDetailsMap).remove("documents");
        }

        Object employeeObj = dtoMap.get("employee");
        if (employeeObj instanceof Map<?, ?> employeeMap) {
            Object personalDetailsObj = ((Map<?, ?>) employeeMap).get("userPersonalDetails");
            if (personalDetailsObj instanceof Map<?, ?> personalDetailsMap) {
                ((Map<String, Object>) personalDetailsMap).remove("birthImg");
                ((Map<String, Object>) personalDetailsMap).remove("maritalStatusDocument");
            }
        }

        Long claimId = claimsRequestResponseDTO.getId();
        boolean generated = claimId != null && activeClaimIds.contains(claimId);
        boolean released = claimId != null && releasedClaimIds.contains(claimId);
        if (generated) {
            dtoMap.put("generatedStatus", "GENERATED");
        } else if (released) {
            dtoMap.put("generatedStatus", "PENDING");
        } else {
            dtoMap.put("generatedStatus", "AVAILABLE");
        }

        return dtoMap;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Date parseDate(String date, boolean startOfDay) {
        if (!hasText(date)) {
            return null;
        }
        try {
            String normalized = date.contains("-") ? date.replace("-", "/") : date;
            return startOfDay ? DateTimeUtil.getStartOfDay(normalized) : DateTimeUtil.getEndOfDay(normalized);
        } catch (Exception e) {
            log.warn("Unable to parse date {}. Falling back to ISO date parser.", date);
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
            return Date.from(localDate.atStartOfDay(TimeZone.getDefault().toZoneId()).toInstant());
        }
    }

    private String buildEmployeeName(UserPersonalDetails personalDetails) {
        String first = Optional.ofNullable(personalDetails.getFirstName()).orElse("");
        String last = Optional.ofNullable(personalDetails.getLastName()).orElse("");
        return (first + " " + last).trim();
    }

    private PaymentAttachmentPrintSummaryDTO buildPrintSummary(PaymentAttachmentResponseDTO responseDTO) {
        PaymentAttachmentPrintSummaryDTO summary = new PaymentAttachmentPrintSummaryDTO();
        List<PaymentAttachmentClaimResponseDTO> claims = Optional.ofNullable(responseDTO.getClaims()).orElse(List.of());
        summary.setTotalClaims(claims.size());
        summary.setTotalRequestedAmount(claims.stream()
                .map(PaymentAttachmentClaimResponseDTO::getRequestAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setTotalApprovedAmount(claims.stream()
                .map(PaymentAttachmentClaimResponseDTO::getApprovedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return summary;
    }

    private WebPage resolveAttachmentPage(PaymentAttachment attachment) {
        if (attachment != null && PaymentAttachmentStatus.FINALIZED.equals(attachment.getStatus())) {
            return WebPage.PASE;
        }
        return WebPage.PARE;
    }

    private WebPage resolveAttachmentListPage(PaymentAttachmentSearchDTO filter) {
        if (filter != null && hasOnlyFinalizedStatus(filter.getStatus())) {
            return WebPage.PASE;
        }
        return WebPage.PARE;
    }

    private boolean hasOnlyFinalizedStatus(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return false;
        }

        boolean hasFinalized = false;
        for (String status : statuses) {
            if (status == null || status.isBlank()) {
                continue;
            }
            if (PaymentAttachmentStatus.FINALIZED.name().equalsIgnoreCase(status.trim())) {
                hasFinalized = true;
            } else {
                return false;
            }
        }
        return hasFinalized;
    }

    private ResponseEntity<ApiResponse<Object>> buildReferenceData(ChannelRequestDTO channelRequestDTO,
                                                                   WebPage page,
                                                                   Locale locale) {
        Map<String, Object> responseMap = new HashMap<>();

        AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                .getPrivileges(channelRequestDTO.getUsername(), page.name());

        responseMap.put("privileges", privileges);
        responseMap.put("defaultStatus", buildDefaultStatus(page));
        responseMap.put("treatmentCategory", loadTreatmentCategories());
        responseMap.put("treatment", loadClaimCategories());
        responseMap.put("company", loadCompanyTypes());
        responseMap.put("staffCategories", loadStaffCategories());

        auditLogService.log(page.name(), WebTask.REF_DATA.name(),
                AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
        return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(
                ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{page.name()}, locale)));
    }

    private List<SimpleBaseDTO> buildDefaultStatus(WebPage page) {
        if (WebPage.PAMC.equals(page)) {
            return Arrays.stream(Workflow.values())
                    .filter(status -> Workflow.APPROVED.equals(status) || Workflow.REJECTED.equals(status))
                    .map(status -> new SimpleBaseDTO(status.name(), status.getDescription()))
                    .toList();
        }

        if (WebPage.PASE.equals(page)) {
            return List.of(new SimpleBaseDTO(PaymentAttachmentStatus.FINALIZED.name(),
                    PaymentAttachmentStatus.FINALIZED.name()));
        }

        return Arrays.stream(PaymentAttachmentStatus.values())
                .map(status -> new SimpleBaseDTO(status.name(), status.name()))
                .toList();
    }

    private List<SimpleBaseDTO> loadTreatmentCategories() {
        return treatmentCategoryRepository.findAllByStatus(Status.ACTIVE)
                .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();
    }

    private List<SimpleBaseDTO> loadClaimCategories() {
        return treatmentRepository.findAllByStatus(Status.ACTIVE)
                .stream().map(val -> new SimpleBaseDTO(val.getTreatmentCode(), val.getTreatmentDescription())).toList();
    }

    private List<SimpleBaseDTO> loadCompanyTypes() {
        return companyTypeRepository.findAllByStatus(Status.ACTIVE)
                .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();
    }

    private List<SimpleBaseDTO> loadStaffCategories() {
        return staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();
    }

    private String buildPrintHtml(PaymentAttachmentResponseDTO responseDTO, PaymentAttachmentPrintSummaryDTO summary) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;color:#222;margin:24px;}")
                .append(".header{display:flex;justify-content:space-between;margin-bottom:16px;}")
                .append(".title{font-size:20px;font-weight:bold;margin-bottom:8px;}")
                .append(".block{width:48%;font-size:12px;line-height:1.6;}")
                .append("table{width:100%;border-collapse:collapse;margin-top:12px;font-size:12px;}")
                .append("th,td{border:1px solid #999;padding:6px;text-align:left;}")
                .append("th{background:#f2f2f2;}")
                .append(".totals{margin-top:12px;font-size:12px;font-weight:bold;}")
                .append(".signatures{margin-top:24px;font-size:12px;display:flex;justify-content:space-between;}")
                .append("</style></head><body>");

        html.append("<div class=\"header\">")
                .append("<div class=\"block\">")
                .append("<div class=\"title\">Payment Attachment</div>")
                .append("<div>Attachment No: ").append(escapeHtml(responseDTO.getAttachmentNo())).append("</div>")
                .append("<div>Date Range: ").append(formatDate(responseDTO.getDateFrom()))
                .append(" &rarr; ").append(formatDate(responseDTO.getDateTo())).append("</div>")
                .append("<div>Status: ").append(escapeHtml(responseDTO.getStatus())).append("</div>")
                .append("<div>Created By: ").append(escapeHtml(responseDTO.getCreatedBy()))
                .append(" (").append(formatDate(responseDTO.getCreatedDate())).append(")</div>")
                .append("</div>")
                .append("<div class=\"block\">")
                .append("<div>Company: ").append(escapeHtml(responseDTO.getCompanyCode())).append("</div>")
                .append("<div>Staff Category: ").append(escapeHtml(responseDTO.getStaffCategoryCode())).append("</div>")
                .append("<div>Treatment Category: ").append(escapeHtml(responseDTO.getTreatmentCategory())).append("</div>")
                .append("<div>Notes: ").append(escapeHtml(responseDTO.getNotes())).append("</div>")
                .append("</div>")
                .append("</div>");

        html.append("<table><thead><tr>")
                .append("<th>#</th>")
                .append("<th>Claim Request ID</th>")
                .append("<th>Employee Name</th>")
                .append("<th>EPF</th>")
                .append("<th>Claim Category</th>")
                .append("<th>Treatment Category</th>")
                .append("<th>Request Amount</th>")
                .append("<th>Approved Amount</th>")
                .append("<th>Status</th>")
                .append("<th>Remark</th>")
                .append("</tr></thead><tbody>");

        List<PaymentAttachmentClaimResponseDTO> claims = Optional.ofNullable(responseDTO.getClaims()).orElse(List.of());
        int index = 1;
        for (PaymentAttachmentClaimResponseDTO claim : claims) {
            html.append("<tr>")
                    .append("<td>").append(index++).append("</td>")
                    .append("<td>").append(escapeHtml(claim.getRequestId())).append("</td>")
                    .append("<td>").append(escapeHtml(claim.getEmployeeName())).append("</td>")
                    .append("<td>").append(escapeHtml(claim.getEpf())).append("</td>")
                    .append("<td>").append(escapeHtml(claim.getClaimCategory())).append("</td>")
                    .append("<td>").append(escapeHtml(claim.getTreatmentCategory())).append("</td>")
                    .append("<td>").append(formatAmount(claim.getRequestAmount())).append("</td>")
                    .append("<td>").append(formatAmount(claim.getApprovedAmount())).append("</td>")
                    .append("<td>").append(escapeHtml(claim.getClaimStatus())).append("</td>")
                    .append("<td>").append(escapeHtml(claim.getRemark())).append("</td>")
                    .append("</tr>");
        }

        html.append("</tbody></table>");

        html.append("<div class=\"totals\">")
                .append("Total Claims: ").append(summary.getTotalClaims())
                .append(" | Total Requested Amount: ").append(formatAmount(summary.getTotalRequestedAmount()))
                .append(" | Total Approved Amount: ").append(formatAmount(summary.getTotalApprovedAmount()))
                .append("</div>");

        html.append("<div class=\"signatures\">")
                .append("<div>Prepared By: __________________</div>")
                .append("<div>Approved By: __________________</div>")
                .append("<div>Date: __________________</div>")
                .append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    private byte[] buildPdf(PaymentAttachmentResponseDTO responseDTO, PaymentAttachmentPrintSummaryDTO summary) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("logo", loadLogo());
            params.put("attachmentNo", responseDTO.getAttachmentNo());
            params.put("dateFrom", formatDate(responseDTO.getDateFrom()));
            params.put("dateTo", formatDate(responseDTO.getDateTo()));
            params.put("status", responseDTO.getStatus());
            params.put("createdBy", responseDTO.getCreatedBy());
            params.put("createdDate", formatDate(responseDTO.getCreatedDate()));
            params.put("companyCode", hasText(responseDTO.getCompanyDescription())
                    ? responseDTO.getCompanyDescription()
                    : responseDTO.getCompanyCode());
            params.put("staffCategoryCode", hasText(responseDTO.getStaffCategoryDescription())
                    ? responseDTO.getStaffCategoryDescription()
                    : responseDTO.getStaffCategoryCode());
            params.put("treatmentCategory", hasText(responseDTO.getTreatmentCategoryDescription())
                    ? responseDTO.getTreatmentCategoryDescription()
                    : responseDTO.getTreatmentCategory());
            params.put("notes", responseDTO.getNotes());
            params.put("totalClaims", summary.getTotalClaims());
            params.put("totalRequestedAmount", formatAmount(summary.getTotalRequestedAmount()));
            params.put("totalApprovedAmount", formatAmount(summary.getTotalApprovedAmount()));

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(
                    Optional.ofNullable(responseDTO.getClaims()).orElse(List.of())
            );

            JasperReport report = JasperCompileManager.compileReport(
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("reports/payment-attachment.jrxml"))
            );
            JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private InputStream loadLogo() {
        String logoPath = "reports/sgcs logo.jpg";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(logoPath)) {
            if (input == null) {
                log.warn("Logo resource not found: {}", logoPath);
                return null;
            }
            return new ByteArrayInputStream(input.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load logo", e);
        }
    }

    private byte[] buildExcel(PaymentAttachmentResponseDTO responseDTO, PaymentAttachmentPrintSummaryDTO summary) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payment Attachment");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle labelStyle = workbook.createCellStyle();
            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            labelStyle.setFont(labelFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            applyBorders(headerStyle);

            CellStyle dataStyle = workbook.createCellStyle();
            applyBorders(dataStyle);

            CellStyle statusStyle = workbook.createCellStyle();
            statusStyle.setAlignment(HorizontalAlignment.CENTER);
            applyBorders(statusStyle);

            CellStyle amountStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            amountStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            amountStyle.setAlignment(HorizontalAlignment.RIGHT);
            applyBorders(amountStyle);

            sheet.setColumnWidth(0, 6 * 256);
            sheet.setColumnWidth(1, 18 * 256);
            sheet.setColumnWidth(2, 10 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            sheet.setColumnWidth(4, 14 * 256);
            sheet.setColumnWidth(5, 14 * 256);
            sheet.setColumnWidth(6, 12 * 256);
            sheet.setColumnWidth(7, 24 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("Payment Attachment");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 7));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "Attachment No", labelStyle);
            createStringCell(row, 1, responseDTO.getAttachmentNo(), null);
            createStringCell(row, 3, "Date Range", labelStyle);
            createStringCell(row, 4, buildDateRange(responseDTO.getDateFrom(), responseDTO.getDateTo()), null);

            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "Company", labelStyle);
            createStringCell(row, 1, responseDTO.getCompanyCode(), null);
            createStringCell(row, 3, "Staff Category", labelStyle);
            createStringCell(row, 4, responseDTO.getStaffCategoryCode(), null);

            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "Treatment Category", labelStyle);
            createStringCell(row, 1, responseDTO.getTreatmentCategory(), null);
            createStringCell(row, 3, "Created By", labelStyle);
            createStringCell(row, 4, buildCreatedBy(responseDTO.getCreatedBy(), responseDTO.getCreatedDate()), null);

            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "Notes", labelStyle);
            createStringCell(row, 1, responseDTO.getNotes(), null);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 1, 7));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Claim ID", headerStyle);
            createStringCell(row, 2, "EPF", headerStyle);
            createStringCell(row, 3, "Company", headerStyle);
            createStringCell(row, 4, "Requested", headerStyle);
            createStringCell(row, 5, "Approved", headerStyle);
            createStringCell(row, 6, "Status", headerStyle);
            createStringCell(row, 7, "Remark", headerStyle);

            List<PaymentAttachmentClaimResponseDTO> claims = Optional.ofNullable(responseDTO.getClaims()).orElse(List.of());
            int lineNo = 1;
            for (PaymentAttachmentClaimResponseDTO claim : claims) {
                row = sheet.createRow(rowIndex++);
                Cell indexCell = row.createCell(0);
                indexCell.setCellValue(lineNo++);
                indexCell.setCellStyle(dataStyle);

                createStringCell(row, 1, claim.getRequestId(), dataStyle);
                createStringCell(row, 2, claim.getEpf(), dataStyle);

                String companyCode = hasText(claim.getCompanyCode()) ? claim.getCompanyCode() : responseDTO.getCompanyCode();
                createStringCell(row, 3, companyCode, dataStyle);

                Cell requestCell = row.createCell(4);
                requestCell.setCellValue(Optional.ofNullable(claim.getRequestAmount()).orElse(BigDecimal.ZERO).doubleValue());
                requestCell.setCellStyle(amountStyle);

                Cell approvedCell = row.createCell(5);
                approvedCell.setCellValue(Optional.ofNullable(claim.getApprovedAmount()).orElse(BigDecimal.ZERO).doubleValue());
                approvedCell.setCellStyle(amountStyle);

                createStringCell(row, 6, claim.getClaimStatus(), statusStyle);

                String remark = "";
                if (claim.getClaimStatus() != null
                        && claim.getClaimStatus().equalsIgnoreCase(PaymentAttachmentStatus.REJECTED.name())
                        && hasText(claim.getRemark())) {
                    remark = claim.getRemark();
                }
                createStringCell(row, 7, remark, dataStyle);
            }

            rowIndex++;
            row = sheet.createRow(rowIndex);
            createStringCell(row, 0, "Total Claims", labelStyle);
            Cell totalClaimsCell = row.createCell(1);
            totalClaimsCell.setCellValue(Optional.ofNullable(summary.getTotalClaims()).orElse(0));
            totalClaimsCell.setCellStyle(dataStyle);

            createStringCell(row, 3, "Total Requested", labelStyle);
            Cell totalRequestedCell = row.createCell(4);
            totalRequestedCell.setCellValue(Optional.ofNullable(summary.getTotalRequestedAmount()).orElse(BigDecimal.ZERO).doubleValue());
            totalRequestedCell.setCellStyle(amountStyle);

            createStringCell(row, 5, "Total Approved", labelStyle);
            Cell totalApprovedCell = row.createCell(6);
            totalApprovedCell.setCellValue(Optional.ofNullable(summary.getTotalApprovedAmount()).orElse(BigDecimal.ZERO).doubleValue());
            totalApprovedCell.setCellStyle(amountStyle);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    private void applyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void createStringCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private String escapeHtml(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    private String buildDateRange(Date from, Date to) {
        String fromText = formatDate(from);
        String toText = formatDate(to);
        if (!hasText(fromText) && !hasText(toText)) {
            return "";
        }
        if (!hasText(fromText)) {
            return toText;
        }
        if (!hasText(toText)) {
            return fromText;
        }
        return fromText + " to " + toText;
    }

    private String buildCreatedBy(String createdBy, Date createdDate) {
        String createdByText = hasText(createdBy) ? createdBy : "";
        String dateText = formatDate(createdDate);
        if (!hasText(createdByText)) {
            return dateText;
        }
        if (!hasText(dateText)) {
            return createdByText;
        }
        return createdByText + " (" + dateText + ")";
    }

    private Map<String, String> loadCompanyDescriptions() {
        return companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                .filter(Objects::nonNull)
                .filter(company -> hasText(company.getCode()))
                .collect(Collectors.toMap(company -> normalizeCode(company.getCode()),
                        CompanyTypes::getDescription,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<String, String> loadStaffCategoryDescriptions() {
        return staffCategoriesRepository.findAllByStatus(Status.ACTIVE).stream()
                .filter(Objects::nonNull)
                .filter(category -> hasText(category.getCode()))
                .collect(Collectors.toMap(category -> normalizeCode(category.getCode()),
                        StaffCategories::getDescription,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<String, String> loadTreatmentCategoryDescriptions() {
        return treatmentCategoryRepository.findAllByStatus(Status.ACTIVE).stream()
                .filter(Objects::nonNull)
                .filter(category -> hasText(category.getCode()))
                .collect(Collectors.toMap(category -> normalizeCode(category.getCode()),
                        TreatmentCategory::getDescription,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<String, String> loadTreatmentDescriptions() {
        return treatmentRepository.findAllByStatus(Status.ACTIVE).stream()
                .filter(Objects::nonNull)
                .filter(treatment -> hasText(treatment.getTreatmentCode()))
                .collect(Collectors.toMap(treatment -> normalizeCode(treatment.getTreatmentCode()),
                        Treatment::getTreatmentDescription,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private String resolveDescription(Map<String, String> descriptions, String code) {
        if (!hasText(code) || descriptions == null) {
            return null;
        }
        return descriptions.get(normalizeCode(code));
    }

    private String resolveTreatmentDescription(String code,
                                               Map<String, String> treatmentCategoryDescriptions,
                                               Map<String, String> treatmentDescriptions) {
        String description = resolveDescription(treatmentCategoryDescriptions, code);
        if (hasText(description)) {
            return description;
        }
        return resolveDescription(treatmentDescriptions, code);
    }

    private String normalizeCode(String code) {
        if (!hasText(code)) {
            return null;
        }
        return code.trim().toUpperCase();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(amount);
    }

    private String safeFileName(String value) {
        if (!hasText(value)) {
            return "attachment";
        }
        return value.replace("/", "-").replace("\\", "-").replace(" ", "_");
    }
}

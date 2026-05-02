package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.PaymentAdviceCreateDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.PaymentAdviceAttachmentResponseDTO;
import com.dtech.admin.dto.response.PaymentAdviceListResponseDTO;
import com.dtech.admin.dto.response.PaymentAdviceResponseDTO;
import com.dtech.admin.dto.response.PaymentAttachmentListResponseDTO;
import com.dtech.admin.dto.search.PaymentAdviceAttachmentSearchDTO;
import com.dtech.admin.dto.search.PaymentAdviceSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.model.PaymentAdviceAttachment;
import com.dtech.admin.model.PaymentAttachment;
import com.dtech.admin.model.PaymentAttachmentClaim;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.PaymentAdviceAttachmentRepository;
import com.dtech.admin.repository.PaymentAdviceRepository;
import com.dtech.admin.repository.PaymentAttachmentClaimRepository;
import com.dtech.admin.repository.PaymentAttachmentRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.PaymentAdviceService;
import com.dtech.admin.specifications.PaymentAdviceAttachmentSpecification;
import com.dtech.admin.specifications.PaymentAdviceSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.MedicalClaimStaffCategoryResolver;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class PaymentAdviceServiceImpl implements PaymentAdviceService {

    private static final String COMPANY_MISMATCH = "__COMPANY_MISMATCH__";
    private static final String STAFF_CATEGORY_MISMATCH = "__STAFF_CATEGORY_MISMATCH__";
    private static final int POLICY_YEAR_MISMATCH = -1;
    private static final String PAGE_CREATE = "PADV_CRE";
    private static final String PAGE_SETTLED = "PADV_SET";
    private static final String VOUCHER_PREFIX = "HC/";
    private static final int VOUCHER_PAD = 7;
    private static final int ADVICE_PAD = 3;
    private static final String DEFAULT_INSURANCE_DEPARTMENT = "Insurance";

    @Autowired
    private final PaymentAttachmentRepository paymentAttachmentRepository;

    @Autowired
    private final PaymentAttachmentClaimRepository paymentAttachmentClaimRepository;

    @Autowired
    private final PaymentAdviceRepository paymentAdviceRepository;

    @Autowired
    private final PaymentAdviceAttachmentRepository paymentAdviceAttachmentRepository;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final Gson gson;

    @Autowired
    private final MedicalClaimStaffCategoryResolver medicalClaimStaffCategoryResolver;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Payment advice reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_CREATE);

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", List.of(new SimpleBaseDTO(
                    PaymentAdviceStatus.FINALIZED.name(), PaymentAdviceStatus.FINALIZED.name())));
            responseMap.put("company", loadCompanyTypes());
            responseMap.put("paymentCompany", loadCompanyTypes());
            responseMap.put("staffCategories", loadStaffCategories());

            auditLogService.log(PAGE_CREATE, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(
                    ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.PADV.name()}, locale)));
        } catch (Exception e) {
            log.error("Failed to load payment advice reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterEligibleAttachments(
            PaginationRequest<PaymentAdviceAttachmentSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Payment advice eligible attachments filter {}", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            PaymentAdviceAttachmentSearchDTO filter = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(PaymentAdviceAttachmentSearchDTO::new);
            String requestedStaffCategoryCode = medicalClaimStaffCategoryResolver
                    .normalizeSelectionCode(filter.getStaffCategory());
            filter.setStaffCategoryCodes(medicalClaimStaffCategoryResolver
                    .expandStoredCodesForFilter(requestedStaffCategoryCode));

            Page<PaymentAttachment> attachments = paymentAttachmentRepository
                    .findAll(PaymentAdviceAttachmentSpecification.getSpecification(filter), pageable);

            long total = paymentAttachmentRepository.count(PaymentAdviceAttachmentSpecification.getSpecification(filter));

            Map<String, String> companyDescriptions = loadCompanyDescriptions();
            Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();

            List<PaymentAttachmentListResponseDTO> response = attachments.stream()
                    .map(attachment -> mapAttachmentToListResponse(attachment, companyDescriptions, staffCategoryDescriptions))
                    .collect(Collectors.toList());

            auditLogService.log(PAGE_CREATE, WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(),
                    paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(response), null,
                    paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(
                    (Object) new PagingResult<>(response, response.size(), total),
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_ATTACHMENT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter eligible payment attachments", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> create(PaymentAdviceCreateDTO paymentAdviceCreateDTO, Locale locale) {
        try {
            log.info("Creating payment advice {}", paymentAdviceCreateDTO);

            List<Long> attachmentIds = Optional.ofNullable(paymentAdviceCreateDTO.getAttachmentIds()).orElse(List.of());
            List<PaymentAttachment> attachments = paymentAttachmentRepository.findAllById(attachmentIds);
            if (attachments.isEmpty() || attachments.size() != attachmentIds.size()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 404,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_ATTACHMENT_NOT_FOUND, null, locale)));
            }

            Optional<PaymentAttachment> notFinalized = attachments.stream()
                    .filter(attachment -> !PaymentAttachmentStatus.FINALIZED.equals(attachment.getStatus()))
                    .findFirst();
            if (notFinalized.isPresent()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1055,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_ATTACHMENT_NOT_FINALIZED,
                                new Object[]{notFinalized.get().getAttachmentNo()}, locale)));
            }

            Optional<PaymentAttachment> alreadyUsed = attachments.stream()
                    .filter(paymentAdviceAttachmentRepository::existsByPaymentAttachment)
                    .findFirst();
            if (alreadyUsed.isPresent()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1056,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_ATTACHMENT_ALREADY_USED,
                                new Object[]{alreadyUsed.get().getAttachmentNo()}, locale)));
            }

            String companyCode = resolveCompanyCode(attachments);
            if (COMPANY_MISMATCH.equals(companyCode)) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1057,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_COMPANY_MISMATCH, null, locale)));
            }

            String staffCategoryCode = resolveStaffCategoryCode(attachments);
            if (STAFF_CATEGORY_MISMATCH.equals(staffCategoryCode)) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1058,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_STAFF_CATEGORY_MISMATCH, null, locale)));
            }

            if (!hasText(staffCategoryCode)) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1059,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_STAFF_CATEGORY_MISSING, null, locale)));
            }

            if (hasText(paymentAdviceCreateDTO.getCompanyCode())
                    && !companyCode.equalsIgnoreCase(paymentAdviceCreateDTO.getCompanyCode())) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1057,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_COMPANY_MISMATCH, null, locale)));
            }

            String requestedStaffCategoryCode = medicalClaimStaffCategoryResolver
                    .normalizeSelectionCode(paymentAdviceCreateDTO.getStaffCategoryCode());
            if (hasText(requestedStaffCategoryCode)
                    && !staffCategoryCode.equalsIgnoreCase(requestedStaffCategoryCode)) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1058,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_STAFF_CATEGORY_MISMATCH, null, locale)));
            }

            Integer yearStart = resolveAdviceYearStart(attachments);
            if (yearStart == null || yearStart == POLICY_YEAR_MISMATCH) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1063,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_POLICY_YEAR_MISMATCH, null, locale)));
            }

            int yearEnd = yearStart + 1;
            int adviceSequence = resolveAdviceSequence(yearStart, staffCategoryCode);
            String adviceNo = buildAdviceNo(yearStart, yearEnd, staffCategoryCode, adviceSequence);

            int voucherSequence = resolveVoucherSequence();
            String voucherNo = buildVoucherNo(voucherSequence);

            AmountSummary totals = new AmountSummary();
            PaymentAdvice advice = new PaymentAdvice();
            advice.setAdviceNo(adviceNo);
            advice.setAdviceYearStart(yearStart);
            advice.setAdviceYearEnd(yearEnd);
            advice.setAdviceSequence(adviceSequence);
            advice.setVoucherNo(voucherNo);
            advice.setVoucherSequence(voucherSequence);
            advice.setType(PaymentAdviceType.MEDICAL);
            advice.setCompanyCode(companyCode);
            advice.setStaffCategoryCode(staffCategoryCode);
            advice.setDepartment(DEFAULT_INSURANCE_DEPARTMENT);
            advice.setStatus(PaymentAdviceStatus.FINALIZED);

            List<PaymentAdviceAttachment> adviceAttachments = attachments.stream()
                    .map(attachment -> buildAdviceAttachment(advice, attachment, totals))
                    .collect(Collectors.toList());

            advice.setTotalRequestedAmount(totals.totalRequested);
            advice.setTotalApprovedAmount(totals.totalApproved);
            advice.setAttachments(adviceAttachments);

            paymentAdviceRepository.saveAndFlush(advice);

            PaymentAdviceResponseDTO responseDTO = mapAdviceToResponse(advice, true);

            auditLogService.log(PAGE_CREATE, WebTask.ADD.name(), AuditTask.ADD_DATA.getDescription(),
                    paymentAdviceCreateDTO.getIp(), paymentAdviceCreateDTO.getUserAgent(), gson.toJson(responseDTO), null,
                    paymentAdviceCreateDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_CREATED_SUCCESS,
                            new Object[]{responseDTO.getAdviceNo()}, locale)));
        } catch (Exception e) {
            log.error("Failed to create payment advice", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<PaymentAdviceSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Payment advice filter {}", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            PaymentAdviceSearchDTO filter = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(PaymentAdviceSearchDTO::new);
            String requestedStaffCategoryCode = medicalClaimStaffCategoryResolver
                    .normalizeSelectionCode(filter.getStaffCategory());
            filter.setStaffCategoryCodes(medicalClaimStaffCategoryResolver
                    .expandStoredCodesForFilter(requestedStaffCategoryCode));

            Page<PaymentAdvice> advicePage = paymentAdviceRepository
                    .findAll(PaymentAdviceSpecification.getSpecification(filter), pageable);
            long total = paymentAdviceRepository.count(PaymentAdviceSpecification.getSpecification(filter));

            Map<String, String> companyDescriptions = loadCompanyDescriptions();
            Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();

            List<PaymentAdviceListResponseDTO> response = advicePage.stream()
                    .map(advice -> mapAdviceToListResponse(advice, companyDescriptions, staffCategoryDescriptions))
                    .collect(Collectors.toList());

            auditLogService.log(PAGE_SETTLED, WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(),
                    paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(response), null,
                    paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<>(response, response.size(), total),
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter payment advice list", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("View payment advice {}", id);
            return paymentAdviceRepository.findById(id)
                    .filter(this::isMedicalAdvice)
                    .map(advice -> {
                        PaymentAdviceResponseDTO responseDTO = mapAdviceToResponse(advice, true);
                        auditLogService.log(PAGE_SETTLED, WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                                requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(responseDTO), null,
                                requestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                                messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_RETRIEVE_SUCCESSFULLY,
                                        null, locale)));
                    })
                    .orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 404,
                            messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_NOT_FOUND,
                                    new Object[]{id}, locale))));
        } catch (Exception e) {
            log.error("Failed to view payment advice {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<String> print(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("Print payment advice {}", id);
            return paymentAdviceRepository.findById(id)
                    .filter(this::isMedicalAdvice)
                    .map(advice -> {
                        PaymentAdviceResponseDTO responseDTO = mapAdviceToResponse(advice, true);
                        String html = buildPrintHtml(responseDTO);

                        auditLogService.log(PAGE_SETTLED, WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                                requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(responseDTO), null,
                                requestDTO.getUsername());

                        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
                    })
                    .orElseGet(() -> ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN)
                            .body("Payment advice not found: " + id));
        } catch (Exception e) {
            log.error("Failed to print payment advice {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> printPdf(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("Print payment advice PDF {}", id);
            return paymentAdviceRepository.findById(id)
                    .filter(this::isMedicalAdvice)
                    .map(advice -> {
                        PaymentAdviceResponseDTO responseDTO = mapAdviceToResponse(advice, true);
                        byte[] pdfBytes = buildPdf(responseDTO);

                        auditLogService.log(PAGE_SETTLED, WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                                requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(responseDTO), null,
                                requestDTO.getUsername());

                        String fileName = "payment-advice-" + safeFileName(responseDTO.getAdviceNo()) + ".pdf";
                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                                .body(pdfBytes);
                    })
                    .orElseGet(() -> ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN)
                            .body(("Payment advice not found: " + id).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Failed to print payment advice PDF {}", id, e);
            throw e;
        }
    }

    private PaymentAdviceAttachment buildAdviceAttachment(PaymentAdvice advice, PaymentAttachment attachment, AmountSummary totals) {
        PaymentAdviceAttachment adviceAttachment = new PaymentAdviceAttachment();
        adviceAttachment.setPaymentAdvice(advice);
        adviceAttachment.setPaymentAttachment(attachment);
        adviceAttachment.setAttachmentNo(attachment.getAttachmentNo());

        AmountSummary attachmentTotals = calculateAttachmentTotals(attachment);
        adviceAttachment.setRequestAmount(attachmentTotals.totalRequested);
        adviceAttachment.setApprovedAmount(attachmentTotals.totalApproved);

        totals.totalRequested = totals.totalRequested.add(attachmentTotals.totalRequested);
        totals.totalApproved = totals.totalApproved.add(attachmentTotals.totalApproved);
        return adviceAttachment;
    }

    private AmountSummary calculateAttachmentTotals(PaymentAttachment attachment) {
        List<PaymentAttachmentClaim> claims = paymentAttachmentClaimRepository.findAllByPaymentAttachment(attachment);
        BigDecimal requested = BigDecimal.ZERO;
        BigDecimal approved = BigDecimal.ZERO;
        for (PaymentAttachmentClaim claim : claims) {
            if (claim.getRequestAmount() != null) {
                requested = requested.add(claim.getRequestAmount());
            }
            if (claim.getApprovedAmount() != null) {
                approved = approved.add(claim.getApprovedAmount());
            }
        }
        AmountSummary summary = new AmountSummary();
        summary.totalRequested = requested;
        summary.totalApproved = approved;
        return summary;
    }

    private PaymentAdviceResponseDTO mapAdviceToResponse(PaymentAdvice advice, boolean includeAttachments) {
        PaymentAdviceResponseDTO dto = new PaymentAdviceResponseDTO();
        dto.setId(advice.getId());
        dto.setAdviceYearStart(advice.getAdviceYearStart());
        dto.setAdviceYearEnd(advice.getAdviceYearEnd());
        dto.setAdviceSequence(advice.getAdviceSequence());
        dto.setAdviceNo(resolveChequeNo(advice));
        dto.setVoucherNo(advice.getVoucherNo());
        dto.setVoucherSequence(advice.getVoucherSequence());
        dto.setCompanyCode(advice.getCompanyCode());
        dto.setStaffCategoryCode(advice.getStaffCategoryCode());
        dto.setDepartment(advice.getDepartment());
        dto.setTotalRequestedAmount(advice.getTotalRequestedAmount());
        dto.setTotalApprovedAmount(advice.getTotalApprovedAmount());
        dto.setStatus(advice.getStatus() != null ? advice.getStatus().name() : null);
        dto.setCreatedDate(advice.getCreatedDate());
        dto.setCreatedBy(advice.getCreatedBy());
        dto.setLastModifiedDate(advice.getLastModifiedDate());
        dto.setLastModifiedBy(advice.getLastModifiedBy());

        Map<String, String> companyDescriptions = loadCompanyDescriptions();
        Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();
        dto.setCompanyDescription(resolveDescription(companyDescriptions, dto.getCompanyCode()));
        dto.setStaffCategoryDescription(resolveDescription(staffCategoryDescriptions, dto.getStaffCategoryCode()));

        if (includeAttachments) {
            List<PaymentAdviceAttachment> attachments = advice.getAttachments().isEmpty()
                    ? paymentAdviceAttachmentRepository.findAllByPaymentAdvice(advice)
                    : advice.getAttachments();

            String paymentCompanyCode = resolvePaymentCompanyCode(attachments);
            dto.setPaymentCompanyCode(paymentCompanyCode);
            dto.setPaymentCompanyDescription(resolveDescription(companyDescriptions, paymentCompanyCode));

            dto.setAttachments(attachments.stream()
                    .map(attachment -> mapAdviceAttachmentToResponse(attachment, companyDescriptions))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private PaymentAdviceAttachmentResponseDTO mapAdviceAttachmentToResponse(PaymentAdviceAttachment attachment,
                                                                             Map<String, String> companyDescriptions) {
        PaymentAdviceAttachmentResponseDTO dto = new PaymentAdviceAttachmentResponseDTO();
        dto.setId(attachment.getId());
        PaymentAttachment paymentAttachment = attachment.getPaymentAttachment();
        dto.setPaymentAttachmentId(paymentAttachment != null ? paymentAttachment.getId() : null);
        dto.setAttachmentNo(attachment.getAttachmentNo());
        String companyCode = resolveAttachmentCompanyCode(paymentAttachment);
        dto.setCompanyCode(companyCode);
        dto.setCompanyDescription(resolveDescription(companyDescriptions, companyCode));
        String paymentCompanyCode = resolvePaymentCompanyCode(paymentAttachment);
        dto.setPaymentCompanyCode(paymentCompanyCode);
        dto.setPaymentCompanyDescription(resolveDescription(companyDescriptions, paymentCompanyCode));
        dto.setRequestAmount(attachment.getRequestAmount());
        dto.setApprovedAmount(attachment.getApprovedAmount());
        return dto;
    }

    private PaymentAdviceListResponseDTO mapAdviceToListResponse(PaymentAdvice advice,
                                                                 Map<String, String> companyDescriptions,
                                                                 Map<String, String> staffCategoryDescriptions) {
        PaymentAdviceListResponseDTO dto = new PaymentAdviceListResponseDTO();
        dto.setId(advice.getId());
        dto.setAdviceNo(resolveChequeNo(advice));
        dto.setVoucherNo(advice.getVoucherNo());
        dto.setCompanyCode(advice.getCompanyCode());
        dto.setStaffCategoryCode(advice.getStaffCategoryCode());
        dto.setTotalRequestedAmount(advice.getTotalRequestedAmount());
        dto.setTotalApprovedAmount(advice.getTotalApprovedAmount());
        dto.setStatus(advice.getStatus() != null ? advice.getStatus().name() : null);
        dto.setCreatedDate(advice.getCreatedDate());
        dto.setCreatedBy(advice.getCreatedBy());
        dto.setCompanyDescription(resolveDescription(companyDescriptions, dto.getCompanyCode()));
        String paymentCompanyCode = resolvePaymentCompanyCode(paymentAdviceAttachmentRepository.findAllByPaymentAdvice(advice));
        dto.setPaymentCompanyCode(paymentCompanyCode);
        dto.setPaymentCompanyDescription(resolveDescription(companyDescriptions, paymentCompanyCode));
        dto.setStaffCategoryDescription(resolveDescription(staffCategoryDescriptions, dto.getStaffCategoryCode()));
        return dto;
    }

    private PaymentAttachmentListResponseDTO mapAttachmentToListResponse(PaymentAttachment attachment,
                                                                         Map<String, String> companyDescriptions,
                                                                         Map<String, String> staffCategoryDescriptions) {
        PaymentAttachmentListResponseDTO dto = new PaymentAttachmentListResponseDTO();
        dto.setId(attachment.getId());
        dto.setAttachmentNo(attachment.getAttachmentNo());
        dto.setStatus(attachment.getStatus() != null ? attachment.getStatus().name() : null);
        dto.setCompanyCode(attachment.getCompanyCode());
        dto.setStaffCategoryCode(attachment.getStaffCategoryCode());
        dto.setTreatmentCategory(attachment.getTreatmentCategory());
        dto.setDateFrom(attachment.getDateFrom());
        dto.setDateTo(attachment.getDateTo());
        dto.setCreatedDate(attachment.getCreatedDate());
        dto.setCreatedBy(attachment.getCreatedBy());
        dto.setCompanyDescription(resolveDescription(companyDescriptions, dto.getCompanyCode()));
        String paymentCompanyCode = resolvePaymentCompanyCode(attachment);
        dto.setPaymentCompanyCode(paymentCompanyCode);
        dto.setPaymentCompanyDescription(resolveDescription(companyDescriptions, paymentCompanyCode));
        dto.setStaffCategoryDescription(resolveDescription(staffCategoryDescriptions, dto.getStaffCategoryCode()));
        return dto;
    }

    private String resolveCompanyCode(List<PaymentAttachment> attachments) {
        String code = null;
        for (PaymentAttachment attachment : attachments) {
            String current = normalizeCode(attachment.getCompanyCode());
            if (!hasText(current)) {
                return COMPANY_MISMATCH;
            }
            if (code == null) {
                code = current;
            } else if (!code.equalsIgnoreCase(current)) {
                return COMPANY_MISMATCH;
            }
        }
        return code;
    }

    private String resolveStaffCategoryCode(List<PaymentAttachment> attachments) {
        String code = null;
        for (PaymentAttachment attachment : attachments) {
            String current = resolveStaffCategoryCodeFromClaims(paymentAttachmentClaimRepository.findAllByPaymentAttachment(attachment));
            if (!hasText(current)) {
                return null;
            }
            if (code == null) {
                code = current;
            } else if (!code.equalsIgnoreCase(current)) {
                return STAFF_CATEGORY_MISMATCH;
            }
        }
        return code;
    }

    private String resolveStaffCategoryCodeFromClaims(List<PaymentAttachmentClaim> attachmentClaims) {
        String code = null;
        for (PaymentAttachmentClaim attachmentClaim : attachmentClaims) {
            String current = Optional.ofNullable(attachmentClaim.getInsuranceClaimsRequest())
                    .map(medicalClaimStaffCategoryResolver::resolveForClaim)
                    .orElseGet(() -> medicalClaimStaffCategoryResolver
                            .normalizeSelectionCode(attachmentClaim.getStaffCategoryCode()));
            if (!hasText(current)) {
                return null;
            }
            if (code == null) {
                code = current;
            } else if (!code.equalsIgnoreCase(current)) {
                return STAFF_CATEGORY_MISMATCH;
            }
        }
        return code;
    }

    private Integer resolveAdviceYearStart(List<PaymentAttachment> attachments) {
        Integer yearStart = null;
        for (PaymentAttachment attachment : attachments) {
            Integer current = attachment.getAttachmentYear();
            if (current == null) {
                return null;
            }
            if (yearStart == null) {
                yearStart = current;
            } else if (!yearStart.equals(current)) {
                return POLICY_YEAR_MISMATCH;
            }
        }
        return yearStart;
    }

    private boolean isMedicalAdvice(PaymentAdvice advice) {
        return advice.getType() == null || PaymentAdviceType.MEDICAL.equals(advice.getType());
    }

    private int resolveAdviceSequence(int yearStart, String staffCategoryCode) {
        return paymentAdviceRepository.findByAdviceYearStartAndStaffCategoryCodeAndTypeOrNullOrderByAdviceSequenceDesc(
                        yearStart, staffCategoryCode, PaymentAdviceType.MEDICAL, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(PaymentAdvice::getAdviceSequence)
                .map(sequence -> sequence + 1)
                .orElse(1);
    }

    private int resolveVoucherSequence() {
        return paymentAdviceRepository.findByTypeOrNullOrderByVoucherSequenceDesc(PaymentAdviceType.MEDICAL, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(PaymentAdvice::getVoucherSequence)
                .map(sequence -> sequence + 1)
                .orElse(1);
    }

    private String buildAdviceNo(int yearStart, int yearEnd, String staffCategoryCode, int sequence) {
        return yearStart + "/" + yearEnd + "/" + staffCategoryCode + "/" + String.format("%0" + ADVICE_PAD + "d", sequence);
    }

    private String buildVoucherNo(int sequence) {
        return VOUCHER_PREFIX + String.format("%0" + VOUCHER_PAD + "d", sequence);
    }

    private String buildPaymentCompany(PaymentAdviceResponseDTO dto) {
        String paymentCompany = hasText(dto.getPaymentCompanyDescription())
                ? dto.getPaymentCompanyDescription()
                : dto.getPaymentCompanyCode();
        String company = hasText(dto.getCompanyDescription()) ? dto.getCompanyDescription() : dto.getCompanyCode();
        if (hasText(paymentCompany)) {
            return paymentCompany;
        }
        return hasText(company) ? company : "";
    }

    private String buildDescription(PaymentAdviceResponseDTO dto) {
        String staff = hasText(dto.getStaffCategoryDescription()) ? dto.getStaffCategoryDescription() : dto.getStaffCategoryCode();
        String company = hasText(dto.getCompanyDescription()) ? dto.getCompanyDescription() : dto.getCompanyCode();
        if (hasText(staff) && hasText(company)) {
            return staff + " Medical for " + company;
        }
        return hasText(staff) ? staff : company;
    }

    private String resolveChequeNo(PaymentAdviceResponseDTO dto) {
        BigDecimal totalApproved = Optional.ofNullable(dto.getTotalApprovedAmount()).orElse(BigDecimal.ZERO);
        if (totalApproved.compareTo(BigDecimal.ZERO) == 0) {
            return buildReturnChequeNo(dto.getCompanyCode(), dto.getStaffCategoryCode(),
                    dto.getAdviceYearStart(), dto.getAdviceSequence());
        }
        return dto.getAdviceNo();
    }

    private String resolveChequeNo(PaymentAdvice advice) {
        BigDecimal totalApproved = Optional.ofNullable(advice.getTotalApprovedAmount()).orElse(BigDecimal.ZERO);
        if (totalApproved.compareTo(BigDecimal.ZERO) == 0) {
            return buildReturnChequeNo(advice.getCompanyCode(), advice.getStaffCategoryCode(),
                    advice.getAdviceYearStart(), advice.getAdviceSequence());
        }
        return advice.getAdviceNo();
    }

    private String buildReturnChequeNo(String companyCode, String staffCategoryCode, Integer adviceYearStart,
                                       Integer adviceSequence) {
        String company = hasText(companyCode) ? companyCode.trim() : "";
        String staff = hasText(staffCategoryCode) ? staffCategoryCode.trim() : "";
        int year = adviceYearStart != null ? adviceYearStart : LocalDate.now().getYear();
        int sequence = adviceSequence != null ? adviceSequence : 1;
        String returnSeq = String.format("%02d", sequence);

        StringBuilder prefix = new StringBuilder();
        if (hasText(company)) {
            prefix.append(company);
        }
        if (hasText(staff)) {
            if (prefix.length() > 0) {
                prefix.append(" ");
            }
            prefix.append(staff);
        }
        if (prefix.length() > 0) {
            prefix.append(" ");
        }
        return prefix + "RETURN-" + year + "-" + returnSeq;
    }

    private String resolveAttachmentCompanyCode(PaymentAttachment paymentAttachment) {
        if (paymentAttachment == null) {
            return null;
        }
        List<PaymentAttachmentClaim> claims = paymentAttachmentClaimRepository.findAllByPaymentAttachment(paymentAttachment);
        String company = null;
        for (PaymentAttachmentClaim claim : claims) {
            String claimCompany = Optional.ofNullable(claim.getCompanyCode())
                    .filter(this::hasText)
                    .orElseGet(() -> Optional.ofNullable(claim.getInsuranceClaimsRequest())
                            .map(InsuranceClaimsRequest::getEmployee)
                            .map(ApplicationUser::getUserPersonalDetails)
                            .map(UserPersonalDetails::getUserCompanyDetails)
                            .map(UserCompanyDetails::getCompanyTypes)
                            .map(CompanyTypes::getCode)
                            .orElse(null));

            if (!hasText(claimCompany)) {
                continue;
            }
            if (company == null) {
                company = claimCompany;
            } else if (!company.equalsIgnoreCase(claimCompany)) {
                return "MULTIPLE";
            }
        }
        return hasText(company) ? company : paymentAttachment.getCompanyCode();
    }

    private String resolvePaymentCompanyCode(PaymentAttachment paymentAttachment) {
        if (paymentAttachment == null) {
            return null;
        }
        List<PaymentAttachmentClaim> claims = paymentAttachmentClaimRepository.findAllByPaymentAttachment(paymentAttachment);
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

    private String resolvePaymentCompanyCode(List<PaymentAdviceAttachment> attachments) {
        String paymentCompany = null;
        for (PaymentAdviceAttachment attachment : attachments) {
            PaymentAttachment paymentAttachment = attachment.getPaymentAttachment();
            if (paymentAttachment == null) {
                continue;
            }

            List<PaymentAttachmentClaim> claims = paymentAttachmentClaimRepository
                    .findAllByPaymentAttachment(paymentAttachment);
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
        }
        return paymentCompany;
    }

    private String buildPrintHtml(PaymentAdviceResponseDTO responseDTO) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;color:#111;margin:24px;}")
                .append(".title{font-size:20px;font-weight:bold;text-align:center;margin-bottom:8px;}")
                .append(".company{display:flex;justify-content:center;gap:12px;font-size:12px;margin-bottom:12px;}");
        html.append(".card{border:1px solid #333;border-collapse:collapse;width:100%;font-size:12px;}")
                .append(".card td{border:1px solid #333;padding:6px;vertical-align:top;}")
                .append(".subtable{width:100%;border-collapse:collapse;}")
                .append(".subtable td{border:none;padding:2px 0;}")
                .append(".amount{text-align:right;}")
                .append(".section{margin-top:12px;}")
                .append(".labels{font-weight:bold;}")
                .append("</style></head><body>");

        html.append("<div class=\"title\">Payment Advice</div>");
        html.append("<div class=\"company\">")
                .append("<div>Company: ").append(escapeHtml(
                        hasText(responseDTO.getCompanyDescription()) ? responseDTO.getCompanyDescription() : responseDTO.getCompanyCode()))
                .append("</div>")
                .append("<div>Address: __________________</div>")
                .append("</div>");

        html.append("<table class=\"card\">");
        html.append("<tr>")
                .append("<td width=\"60%\">")
                .append("<div><span class=\"labels\">Original Company: </span>")
                .append(escapeHtml(hasText(responseDTO.getCompanyDescription())
                        ? responseDTO.getCompanyDescription() : responseDTO.getCompanyCode()))
                .append("</div>")
                .append("<div><span class=\"labels\">Payment Company: </span>")
                .append(escapeHtml(buildPaymentCompany(responseDTO)))
                .append("</div>")
                .append("</td>")
                .append("<td width=\"40%\">")
                .append("<table class=\"subtable\">")
                .append("<tr><td class=\"labels\">Payment Voucher:</td><td>")
                .append(escapeHtml(responseDTO.getVoucherNo()))
                .append("</td></tr>")
                .append("<tr><td class=\"labels\">Cheque No:</td><td>")
                .append(escapeHtml(resolveChequeNo(responseDTO)))
                .append("</td></tr>")
                .append("<tr><td class=\"labels\">Date:</td><td>")
                .append(escapeHtml(formatDate(responseDTO.getCreatedDate())))
                .append("</td></tr>")
                .append("</table>")
                .append("</td>")
                .append("</tr>");

        html.append("<tr>")
                .append("<td>")
                .append("<span class=\"labels\">Details/Description: </span>")
                .append(escapeHtml(buildDescription(responseDTO)))
                .append("</td>")
                .append("<td>")
                .append("<table class=\"subtable\">")
                .append("<tr><td class=\"labels\">Company</td><td class=\"labels amount\">Amount (Rs)</td></tr>")
                .append("</table>")
                .append("</td>")
                .append("</tr>");

        List<PaymentAdviceAttachmentResponseDTO> attachments = Optional.ofNullable(responseDTO.getAttachments()).orElse(List.of());
        for (PaymentAdviceAttachmentResponseDTO attachment : attachments) {
            html.append("<tr>")
                    .append("<td>").append(escapeHtml(attachment.getAttachmentNo())).append("</td>")
                    .append("<td>")
                    .append("<table class=\"subtable\">")
                    .append("<tr><td>")
                    .append(escapeHtml(hasText(attachment.getCompanyDescription())
                            ? attachment.getCompanyDescription() : attachment.getCompanyCode()))
                    .append("</td><td class=\"amount\">")
                    .append(formatAmount(attachment.getApprovedAmount()))
                    .append("</td></tr>")
                    .append("</table>")
                    .append("</td>")
                    .append("</tr>");
        }
        html.append("</table>");

        html.append("<table class=\"card section\">");
        html.append("<tr>")
                .append("<td width=\"50%\">")
                .append("<table class=\"subtable\">")
                .append("<tr><td class=\"labels\">Insurance:</td><td>")
                .append(escapeHtml(responseDTO.getDepartment())).append("</td></tr>")
                .append("</table>")
                .append("</td>")
                .append("<td width=\"50%\">")
                .append("<table class=\"subtable\">")
                .append("<tr><td class=\"labels\">Total Amount:</td><td class=\"amount\">")
                .append(formatAmount(responseDTO.getTotalApprovedAmount())).append("</td></tr>")
                .append("<tr><td class=\"labels\">All Claims Total:</td><td class=\"amount\">")
                .append(formatAmount(responseDTO.getTotalRequestedAmount())).append("</td></tr>")
                .append("</table>")
                .append("</td>")
                .append("</tr>");

        html.append("<tr>")
                .append("<td width=\"50%\">")
                .append("<table class=\"subtable\">")
                .append("<tr><td class=\"labels\">Prepared By:</td><td>__________________</td></tr>")
                .append("<tr><td class=\"labels\">Checked By:</td><td>__________________</td></tr>")
                .append("</table>")
                .append("</td>")
                .append("<td width=\"50%\">")
                .append("<table class=\"subtable\">")
                .append("<tr><td class=\"labels\">Authorized By:</td><td>__________________</td></tr>")
                .append("<tr><td class=\"labels\">Received By:</td><td>__________________</td></tr>")
                .append("</table>")
                .append("</td>")
                .append("</tr>");
        html.append("</table>");

        html.append("</body></html>");
        return html.toString();
    }

    private byte[] buildPdf(PaymentAdviceResponseDTO responseDTO) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("logo", loadLogo());
            params.put("adviceNo", responseDTO.getAdviceNo());
            params.put("chequeNo", resolveChequeNo(responseDTO));
            params.put("voucherNo", responseDTO.getVoucherNo());
            params.put("adviceDate", formatDate(responseDTO.getCreatedDate()));
            params.put("company", hasText(responseDTO.getCompanyDescription())
                    ? responseDTO.getCompanyDescription() : responseDTO.getCompanyCode());
            params.put("paymentCompany", buildPaymentCompany(responseDTO));
            params.put("description", buildDescription(responseDTO));
            String department = responseDTO.getDepartment();
            if (department != null && department.equalsIgnoreCase("Insurance")) {
                department = "Healthcare";
            }
            params.put("department", department);
            params.put("totalApprovedAmount", formatAmount(responseDTO.getTotalApprovedAmount()));
            params.put("totalRequestedAmount", formatAmount(responseDTO.getTotalRequestedAmount()));
            params.put("preparedBy", responseDTO.getCreatedBy());

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(
                    Optional.ofNullable(responseDTO.getAttachments()).orElse(List.of())
            );

            JasperReport report = JasperCompileManager.compileReport(
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("reports/payment-advice.jrxml"))
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

    private List<SimpleBaseDTO> loadCompanyTypes() {
        return companyTypeRepository.findAllByStatus(Status.ACTIVE)
                .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();
    }

    private List<SimpleBaseDTO> loadStaffCategories() {
        return medicalClaimStaffCategoryResolver.loadReferenceCategories();
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
        return medicalClaimStaffCategoryResolver.loadDescriptionMap();
    }

    private String resolveDescription(Map<String, String> descriptions, String code) {
        if (!hasText(code) || descriptions == null) {
            return null;
        }
        if ("MULTIPLE".equalsIgnoreCase(code)) {
            return "Multiple";
        }
        return descriptions.get(normalizeCode(code));
    }

    private String normalizeCode(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(amount);
    }

    private String safeFileName(String value) {
        if (!hasText(value)) {
            return "payment-advice";
        }
        return value.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    private static class AmountSummary {
        private BigDecimal totalRequested = BigDecimal.ZERO;
        private BigDecimal totalApproved = BigDecimal.ZERO;
    }
}

package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.PaymentAdviceDeathCreateDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.PaymentAdviceDeathClaimListResponseDTO;
import com.dtech.admin.dto.response.PaymentAdviceDeathClaimResponseDTO;
import com.dtech.admin.dto.response.PaymentAdviceDeathListResponseDTO;
import com.dtech.admin.dto.response.PaymentAdviceDeathResponseDTO;
import com.dtech.admin.dto.search.PaymentAdviceDeathClaimSearchDTO;
import com.dtech.admin.dto.search.PaymentAdviceDeathSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.PaymentAdviceStatus;
import com.dtech.admin.enums.PaymentAdviceType;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.DeathClaimRequest;
import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.model.PaymentAdviceDeathClaim;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.DeathClaimRequestRepository;
import com.dtech.admin.repository.PaymentAdviceDeathClaimRepository;
import com.dtech.admin.repository.PaymentAdviceRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.PaymentAdviceDeathService;
import com.dtech.admin.specifications.PaymentAdviceDeathClaimSpecification;
import com.dtech.admin.specifications.PaymentAdviceDeathSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class PaymentAdviceDeathServiceImpl implements PaymentAdviceDeathService {
    private static final String COMPANY_MISMATCH = "__COMPANY_MISMATCH__";
    private static final String STAFF_CATEGORY_MISMATCH = "__STAFF_CATEGORY_MISMATCH__";
    private static final String PAGE_CREATE = "PADV_DCRE";
    private static final String PAGE_SETTLED = "PADV_DSET";
    private static final String VOUCHER_PREFIX = "DDF";
    private static final int VOUCHER_PAD = 6;
    private static final int CHEQUE_PAD = 3;
    private static final int ADVICE_PAD = 7;
    private static final String DEFAULT_INSURANCE_DEPARTMENT = "Insurance";

    @Autowired
    private final PaymentAdviceRepository paymentAdviceRepository;

    @Autowired
    private final PaymentAdviceDeathClaimRepository paymentAdviceDeathClaimRepository;

    @Autowired
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Payment advice death reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_CREATE);

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", List.of(
                    new SimpleBaseDTO(Workflow.APPROVED.name(), Workflow.APPROVED.getDescription()),
                    new SimpleBaseDTO(Workflow.REJECTED.name(), Workflow.REJECTED.getDescription())
            ));
            responseMap.put("company", loadCompanyTypes());
            responseMap.put("staffCategories", loadStaffCategories());

            auditLogService.log(PAGE_CREATE, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(
                    ResponseMessageUtil.PAYMENT_ADVICE_DEATH_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load death payment advice reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterEligibleClaims(
            PaginationRequest<PaymentAdviceDeathClaimSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Payment advice death eligible claims filter {}", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            PaymentAdviceDeathClaimSearchDTO filter = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(PaymentAdviceDeathClaimSearchDTO::new);

            Page<DeathClaimRequest> claims = deathClaimRequestRepository
                    .findAll(PaymentAdviceDeathClaimSpecification.getSpecification(filter), pageable);

            long total = deathClaimRequestRepository.count(PaymentAdviceDeathClaimSpecification.getSpecification(filter));

            Map<String, String> companyDescriptions = loadCompanyDescriptions();
            Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();

            List<PaymentAdviceDeathClaimListResponseDTO> response = claims.stream()
                    .map(claim -> mapClaimToListResponse(claim, companyDescriptions, staffCategoryDescriptions))
                    .collect(Collectors.toList());

            auditLogService.log(PAGE_CREATE, WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(),
                    paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(response), null,
                    paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(
                    (Object) new PagingResult<>(response, response.size(), total),
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_CLAIM_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter eligible death claims", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> create(PaymentAdviceDeathCreateDTO paymentAdviceCreateDTO, Locale locale) {
        try {
            log.info("Creating death payment advice {}", paymentAdviceCreateDTO);

            List<Long> claimIds = Optional.ofNullable(paymentAdviceCreateDTO.getDeathClaimIds()).orElse(List.of());
            List<DeathClaimRequest> claims = deathClaimRequestRepository.findAllById(claimIds);
            if (claims.isEmpty() || claims.size() != claimIds.size()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 404,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_CLAIM_NOT_FOUND, null, locale)));
            }

            Optional<DeathClaimRequest> invalidStatus = claims.stream()
                    .filter(claim -> !isEligibleStatus(claim.getRequestStatus()))
                    .findFirst();
            if (invalidStatus.isPresent()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1060,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_CLAIM_INVALID_STATUS,
                                new Object[]{invalidStatus.get().getRequestId()}, locale)));
            }

            Optional<DeathClaimRequest> alreadyUsed = claims.stream()
                    .filter(paymentAdviceDeathClaimRepository::existsByDeathClaim)
                    .findFirst();
            if (alreadyUsed.isPresent()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1061,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_CLAIM_ALREADY_USED,
                                new Object[]{alreadyUsed.get().getRequestId()}, locale)));
            }

            String paymentCompanyCode = resolvePaymentCompanyCode(claims);
            if (COMPANY_MISMATCH.equals(paymentCompanyCode)) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1062,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_COMPANY_MISMATCH, null, locale)));
            }

            String staffCategoryCode = resolveStaffCategoryCode(claims);

            if (hasText(paymentAdviceCreateDTO.getPaymentCompanyCode())
                    && !paymentCompanyCode.equalsIgnoreCase(paymentAdviceCreateDTO.getPaymentCompanyCode())) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1062,
                        messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_COMPANY_MISMATCH, null, locale)));
            }

            int yearStart = LocalDate.now().getYear();
            int yearEnd = yearStart + 1;
            int adviceSequence = resolveAdviceSequence();
            String adviceNo = buildAdviceNo(adviceSequence);

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
            advice.setType(PaymentAdviceType.DEATH);
            advice.setCompanyCode(paymentCompanyCode);
            advice.setStaffCategoryCode(staffCategoryCode);
            advice.setDepartment(DEFAULT_INSURANCE_DEPARTMENT);
            advice.setStatus(PaymentAdviceStatus.FINALIZED);

            paymentAdviceRepository.saveAndFlush(advice);

            List<PaymentAdviceDeathClaim> adviceClaims = claims.stream()
                    .map(claim -> buildAdviceClaim(advice, claim, totals))
                    .collect(Collectors.toList());
            paymentAdviceDeathClaimRepository.saveAll(adviceClaims);

            advice.setTotalRequestedAmount(totals.totalApproved);
            advice.setTotalApprovedAmount(totals.totalApproved);
            paymentAdviceRepository.saveAndFlush(advice);

            PaymentAdviceDeathResponseDTO responseDTO = mapAdviceToResponse(advice, true);

            auditLogService.log(PAGE_CREATE, WebTask.ADD.name(), AuditTask.ADD_DATA.getDescription(),
                    paymentAdviceCreateDTO.getIp(), paymentAdviceCreateDTO.getUserAgent(), gson.toJson(responseDTO), null,
                    paymentAdviceCreateDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_CREATED_SUCCESS,
                            new Object[]{adviceNo}, locale)));
        } catch (Exception e) {
            log.error("Failed to create death payment advice", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<PaymentAdviceDeathSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Payment advice death filter {}", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            PaymentAdviceDeathSearchDTO filter = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(PaymentAdviceDeathSearchDTO::new);

            Page<PaymentAdvice> advicePage = paymentAdviceRepository
                    .findAll(PaymentAdviceDeathSpecification.getSpecification(filter), pageable);
            long total = paymentAdviceRepository.count(PaymentAdviceDeathSpecification.getSpecification(filter));

            Map<String, String> companyDescriptions = loadCompanyDescriptions();
            Map<String, String> staffCategoryDescriptions = loadStaffCategoryDescriptions();

            List<PaymentAdviceDeathListResponseDTO> response = advicePage.stream()
                    .map(advice -> mapAdviceToListResponse(advice, companyDescriptions, staffCategoryDescriptions))
                    .collect(Collectors.toList());

            auditLogService.log(PAGE_SETTLED, WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(),
                    paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(response), null,
                    paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<>(response, response.size(), total),
                    messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter death payment advice list", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("View death payment advice {}", id);
            return paymentAdviceRepository.findById(id)
                    .filter(this::isDeathAdvice)
                    .map(advice -> {
                        PaymentAdviceDeathResponseDTO responseDTO = mapAdviceToResponse(advice, true);
                        auditLogService.log(PAGE_SETTLED, WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                                requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(responseDTO), null,
                                requestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                                messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_RETRIEVE_SUCCESSFULLY,
                                        null, locale)));
                    })
                    .orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 404,
                            messageSource.getMessage(ResponseMessageUtil.PAYMENT_ADVICE_DEATH_NOT_FOUND,
                                    new Object[]{id}, locale))));
        } catch (Exception e) {
            log.error("Failed to view death payment advice {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<String> print(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("Print death payment advice {}", id);
            return paymentAdviceRepository.findById(id)
                    .filter(this::isDeathAdvice)
                    .map(advice -> {
                        PaymentAdviceDeathResponseDTO responseDTO = mapAdviceToResponse(advice, true);
                        String html = buildPrintHtml(responseDTO);

                        auditLogService.log(PAGE_SETTLED, WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                                requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(responseDTO), null,
                                requestDTO.getUsername());

                        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
                    })
                    .orElseGet(() -> ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN)
                            .body("Payment advice not found: " + id));
        } catch (Exception e) {
            log.error("Failed to print death payment advice {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> printPdf(ChannelRequestDTO requestDTO, Long id, Locale locale) {
        try {
            log.info("Print death payment advice PDF {}", id);
            return paymentAdviceRepository.findById(id)
                    .filter(this::isDeathAdvice)
                    .map(advice -> {
                        PaymentAdviceDeathResponseDTO responseDTO = mapAdviceToResponse(advice, true);
                        byte[] pdfBytes = buildPdf(responseDTO);

                        auditLogService.log(PAGE_SETTLED, WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                                requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(responseDTO), null,
                                requestDTO.getUsername());

                        String fileName = "payment-advice-death-" + safeFileName(responseDTO.getAdviceNo()) + ".pdf";
                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                                .body(pdfBytes);
                    })
                    .orElseGet(() -> ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN)
                            .body(("Payment advice not found: " + id).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Failed to print death payment advice PDF {}", id, e);
            throw e;
        }
    }

    private List<SimpleBaseDTO> loadCompanyTypes() {
        return companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(c -> new SimpleBaseDTO(c.getCode(), c.getDescription()))
                .collect(Collectors.toList());
    }

    private List<SimpleBaseDTO> loadStaffCategories() {
        return staffCategoriesRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(c -> new SimpleBaseDTO(c.getCode(), c.getDescription()))
                .collect(Collectors.toList());
    }

    private Map<String, String> loadCompanyDescriptions() {
        return companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                .collect(Collectors.toMap(CompanyTypes::getCode, CompanyTypes::getDescription, (a, b) -> a));
    }

    private Map<String, String> loadStaffCategoryDescriptions() {
        return staffCategoriesRepository.findAllByStatus(Status.ACTIVE).stream()
                .collect(Collectors.toMap(StaffCategories::getCode, StaffCategories::getDescription, (a, b) -> a));
    }

    private PaymentAdviceDeathClaimListResponseDTO mapClaimToListResponse(
            DeathClaimRequest claim, Map<String, String> companyDescriptions, Map<String, String> staffDescriptions) {
        PaymentAdviceDeathClaimListResponseDTO dto = new PaymentAdviceDeathClaimListResponseDTO();
        dto.setId(claim.getId());
        dto.setRequestId(claim.getRequestId());
        dto.setEpf(resolveEmployeeEpf(claim.getEmployee()));
        dto.setEmployeeName(resolveEmployeeName(claim.getEmployee()));
        dto.setDependentName(resolveDependentName(claim));
        dto.setRelation(resolveRelation(claim));
        dto.setApprovedAmount(defaultAmount(claim.getApprovedAmount()));
        dto.setStatus(claim.getRequestStatus().name());

        String companyCode = resolvePaymentCompanyCode(claim);
        dto.setPaymentCompanyCode(companyCode);
        dto.setPaymentCompanyDescription(companyDescriptions.get(companyCode));

        String staffCategoryCode = resolveStaffCategoryCode(claim);
        dto.setStaffCategoryCode(staffCategoryCode);
        dto.setStaffCategoryDescription(staffDescriptions.get(staffCategoryCode));
        return dto;
    }

    private PaymentAdviceDeathListResponseDTO mapAdviceToListResponse(
            PaymentAdvice advice, Map<String, String> companyDescriptions, Map<String, String> staffCategoryDescriptions) {
        PaymentAdviceDeathListResponseDTO dto = new PaymentAdviceDeathListResponseDTO();
        dto.setId(advice.getId());
        dto.setAdviceNo(advice.getAdviceNo());
        dto.setVoucherNo(advice.getVoucherNo());
        dto.setChequeNo(buildChequeNo(advice.getVoucherSequence()));
        dto.setPaymentCompanyCode(advice.getCompanyCode());
        dto.setPaymentCompanyDescription(companyDescriptions.get(advice.getCompanyCode()));
        dto.setStaffCategoryCode(advice.getStaffCategoryCode());
        dto.setStaffCategoryDescription(staffCategoryDescriptions.get(advice.getStaffCategoryCode()));
        dto.setTotalApprovedAmount(advice.getTotalApprovedAmount());
        dto.setStatus(advice.getStatus().name());
        dto.setCreatedDate(advice.getCreatedDate());
        return dto;
    }

    private PaymentAdviceDeathResponseDTO mapAdviceToResponse(PaymentAdvice advice, boolean includeClaims) {
        PaymentAdviceDeathResponseDTO dto = new PaymentAdviceDeathResponseDTO();
        dto.setId(advice.getId());
        dto.setAdviceNo(advice.getAdviceNo());
        dto.setAdviceYearStart(advice.getAdviceYearStart());
        dto.setAdviceYearEnd(advice.getAdviceYearEnd());
        dto.setAdviceSequence(advice.getAdviceSequence());
        dto.setVoucherNo(advice.getVoucherNo());
        dto.setVoucherSequence(advice.getVoucherSequence());
        dto.setChequeNo(buildChequeNo(advice.getVoucherSequence()));
        dto.setPaymentCompanyCode(advice.getCompanyCode());
        dto.setStaffCategoryCode(advice.getStaffCategoryCode());
        dto.setDepartment(advice.getDepartment());
        dto.setTotalApprovedAmount(advice.getTotalApprovedAmount());
        dto.setTotalRequestedAmount(advice.getTotalRequestedAmount());
        dto.setStatus(advice.getStatus().name());
        dto.setCreatedDate(advice.getCreatedDate());
        dto.setCreatedBy(advice.getCreatedBy());
        dto.setLastModifiedDate(advice.getLastModifiedDate());
        dto.setLastModifiedBy(advice.getLastModifiedBy());

        if (includeClaims) {
            List<PaymentAdviceDeathClaimResponseDTO> claims = paymentAdviceDeathClaimRepository
                    .findAllByPaymentAdvice(advice).stream()
                    .map(this::mapAdviceClaimToResponse)
                    .collect(Collectors.toList());
            dto.setClaims(claims);
        }

        Map<String, String> companyDescriptions = loadCompanyDescriptions();
        Map<String, String> staffDescriptions = loadStaffCategoryDescriptions();
        dto.setPaymentCompanyDescription(companyDescriptions.get(advice.getCompanyCode()));
        dto.setStaffCategoryDescription(staffDescriptions.get(advice.getStaffCategoryCode()));
        return dto;
    }

    private PaymentAdviceDeathClaimResponseDTO mapAdviceClaimToResponse(PaymentAdviceDeathClaim adviceClaim) {
        PaymentAdviceDeathClaimResponseDTO dto = new PaymentAdviceDeathClaimResponseDTO();
        DeathClaimRequest claim = adviceClaim.getDeathClaim();
        dto.setId(adviceClaim.getId());
        dto.setDeathClaimId(claim.getId());
        dto.setRequestId(adviceClaim.getRequestId());
        dto.setEpf(resolveEmployeeEpf(claim.getEmployee()));
        dto.setEmployeeName(resolveEmployeeName(claim.getEmployee()));
        dto.setDependentName(resolveDependentName(claim));
        dto.setRelation(resolveRelation(claim));
        dto.setApprovedAmount(defaultAmount(adviceClaim.getApprovedAmount()));
        dto.setClaimStatus(claim.getRequestStatus().name());
        dto.setRemark(claim.getRemark());
        return dto;
    }

    private PaymentAdviceDeathClaim buildAdviceClaim(PaymentAdvice advice, DeathClaimRequest claim, AmountSummary totals) {
        PaymentAdviceDeathClaim adviceClaim = new PaymentAdviceDeathClaim();
        adviceClaim.setPaymentAdvice(advice);
        adviceClaim.setDeathClaim(claim);
        adviceClaim.setRequestId(claim.getRequestId());
        adviceClaim.setApprovedAmount(defaultAmount(claim.getApprovedAmount()));

        totals.totalApproved = totals.totalApproved.add(defaultAmount(claim.getApprovedAmount()));
        return adviceClaim;
    }

    private boolean isDeathAdvice(PaymentAdvice advice) {
        return PaymentAdviceType.DEATH.equals(advice.getType());
    }

    private boolean isEligibleStatus(Workflow workflow) {
        return Workflow.APPROVED.equals(workflow) || Workflow.REJECTED.equals(workflow);
    }

    private String resolvePaymentCompanyCode(List<DeathClaimRequest> claims) {
        String code = null;
        for (DeathClaimRequest claim : claims) {
            String current = resolvePaymentCompanyCode(claim);
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

    private String resolvePaymentCompanyCode(DeathClaimRequest claim) {
        return Optional.ofNullable(claim.getEmployee())
                .map(ApplicationUser::getUserPersonalDetails)
                .map(UserPersonalDetails::getUserCompanyDetails)
                .map(userCompanyDetails -> Optional.ofNullable(userCompanyDetails.getPaymentCompany())
                        .orElse(userCompanyDetails.getCompanyTypes()))
                .map(CompanyTypes::getCode)
                .orElse(null);
    }

    private String resolveStaffCategoryCode(List<DeathClaimRequest> claims) {
        String code = null;
        for (DeathClaimRequest claim : claims) {
            String current = resolveStaffCategoryCode(claim);
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

    private String resolveStaffCategoryCode(DeathClaimRequest claim) {
        return Optional.ofNullable(claim.getEmployee())
                .map(ApplicationUser::getUserPersonalDetails)
                .map(UserPersonalDetails::getUserCompanyDetails)
                .map(UserCompanyDetails::getStaffCategories)
                .map(StaffCategories::getCode)
                .orElse(null);
    }

    private int resolveAdviceSequence() {
        return paymentAdviceRepository.findTopByTypeOrderByAdviceSequenceDesc(PaymentAdviceType.DEATH)
                .map(PaymentAdvice::getAdviceSequence)
                .map(sequence -> sequence + 1)
                .orElse(1);
    }

    private int resolveVoucherSequence() {
        return paymentAdviceRepository.findTopByTypeOrderByVoucherSequenceDesc(PaymentAdviceType.DEATH)
                .map(PaymentAdvice::getVoucherSequence)
                .map(sequence -> sequence + 1)
                .orElse(1);
    }

    private String buildAdviceNo(int sequence) {
        return VOUCHER_PREFIX + String.format("%0" + ADVICE_PAD + "d", sequence);
    }

    private String buildVoucherNo(int sequence) {
        return VOUCHER_PREFIX + String.format("%0" + VOUCHER_PAD + "d", sequence);
    }

    private String buildChequeNo(int sequence) {
        return VOUCHER_PREFIX + String.format("%0" + CHEQUE_PAD + "d", sequence);
    }

    private String buildDescription(PaymentAdviceDeathResponseDTO dto) {
        String staff = hasText(dto.getStaffCategoryDescription()) ? dto.getStaffCategoryDescription() : dto.getStaffCategoryCode();
        String company = hasText(dto.getPaymentCompanyDescription()) ? dto.getPaymentCompanyDescription() : dto.getPaymentCompanyCode();
        if (hasText(staff) && hasText(company)) {
            return staff + " Death Claims for " + company;
        }
        return hasText(staff) ? staff : company;
    }

    private String resolveEmployeeName(ApplicationUser user) {
        if (user == null || user.getUserPersonalDetails() == null) {
            return null;
        }
        UserPersonalDetails details = user.getUserPersonalDetails();
        String first = Optional.ofNullable(details.getFirstName()).orElse("");
        String last = Optional.ofNullable(details.getLastName()).orElse("");
        return (first + " " + last).trim();
    }

    private String resolveEmployeeEpf(ApplicationUser user) {
        if (user == null || user.getUserPersonalDetails() == null) {
            return null;
        }
        return user.getUserPersonalDetails().getEpfNo();
    }

    private String resolveDependentName(DeathClaimRequest claim) {
        ClaimsDependents dependent = claim.getClaimsDependents();
        if (dependent == null) {
            return resolveEmployeeName(claim.getEmployee());
        }
        String initials = Optional.ofNullable(dependent.getInitials()).orElse("");
        String first = Optional.ofNullable(dependent.getFirstName()).orElse("");
        String last = Optional.ofNullable(dependent.getLastName()).orElse("");
        return (initials + " " + first + " " + last).trim();
    }

    private String resolveRelation(DeathClaimRequest claim) {
        ClaimsDependents dependent = claim.getClaimsDependents();
        if (dependent == null) {
            return "EMPLOYEE";
        }
        return dependent.getRelationCategory() != null
                ? dependent.getRelationCategory().getDescription()
                : null;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String formatAmount(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00");
        return format.format(defaultAmount(amount));
    }

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String safeFileName(String value) {
        return value == null ? "advice" : value.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private String buildPrintHtml(PaymentAdviceDeathResponseDTO responseDTO) {
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
                        hasText(responseDTO.getPaymentCompanyDescription()) ? responseDTO.getPaymentCompanyDescription()
                                : responseDTO.getPaymentCompanyCode()))
                .append("</div>")
                .append("<div>Address: __________________</div>")
                .append("</div>");

        html.append("<table class=\"card\">");
        html.append("<tr>")
                .append("<td width=\"60%\">")
                .append("<div><span class=\"labels\">Payment Company: </span>")
                .append(escapeHtml(hasText(responseDTO.getPaymentCompanyDescription())
                        ? responseDTO.getPaymentCompanyDescription() : responseDTO.getPaymentCompanyCode()))
                .append("</div>")
                .append("</td>")
                .append("<td width=\"40%\">")
                .append("<table class=\"subtable\">")
                .append("<tr><td class=\"labels\">Payment Voucher:</td><td>")
                .append(escapeHtml(responseDTO.getVoucherNo()))
                .append("</td></tr>")
                .append("<tr><td class=\"labels\">Cheque No:</td><td>")
                .append(escapeHtml(responseDTO.getChequeNo()))
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
                .append("<td class=\"labels\">Amount (Rs)</td>")
                .append("</tr>");

        List<PaymentAdviceDeathClaimResponseDTO> claims = Optional.ofNullable(responseDTO.getClaims()).orElse(List.of());
        for (PaymentAdviceDeathClaimResponseDTO claim : claims) {
            html.append("<tr>")
                    .append("<td>")
                    .append("<div><span class=\"labels\">Request ID: </span>").append(escapeHtml(claim.getRequestId())).append("</div>")
                    .append("<div><span class=\"labels\">Employee: </span>").append(escapeHtml(claim.getEmployeeName())).append("</div>")
                    .append("<div><span class=\"labels\">Dependent: </span>").append(escapeHtml(claim.getDependentName())).append("</div>")
                    .append("<div><span class=\"labels\">Relation: </span>").append(escapeHtml(claim.getRelation())).append("</div>")
                    .append("</td>")
                    .append("<td class=\"amount\">").append(formatAmount(claim.getApprovedAmount())).append("</td>")
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

    private byte[] buildPdf(PaymentAdviceDeathResponseDTO responseDTO) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("logo", loadLogo());
            params.put("adviceNo", responseDTO.getAdviceNo());
            params.put("voucherNo", responseDTO.getVoucherNo());
            params.put("chequeNo", responseDTO.getChequeNo());
            params.put("adviceDate", formatDate(responseDTO.getCreatedDate()));
            params.put("paymentCompany", hasText(responseDTO.getPaymentCompanyDescription())
                    ? responseDTO.getPaymentCompanyDescription() : responseDTO.getPaymentCompanyCode());
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
                    Optional.ofNullable(responseDTO.getClaims()).orElse(List.of())
            );

            JasperReport report = JasperCompileManager.compileReport(
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("reports/payment-advice-death.jrxml"))
            );
            JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception e) {
            log.error("Failed to generate death payment advice PDF", e);
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

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static class AmountSummary {
        private BigDecimal totalApproved = BigDecimal.ZERO;
    }
}

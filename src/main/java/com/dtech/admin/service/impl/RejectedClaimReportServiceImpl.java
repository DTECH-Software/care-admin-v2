package com.dtech.admin.service.impl;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.RejectedClaimReportCompanyDTO;
import com.dtech.admin.dto.response.RejectedClaimReportPeriodDTO;
import com.dtech.admin.dto.response.RejectedClaimReportReasonDTO;
import com.dtech.admin.dto.response.RejectedClaimReportResponseDTO;
import com.dtech.admin.dto.search.RejectedClaimReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.RemarkCategory;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.ApprovalWorkflowRejectReason;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsDetails;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.model.Remark;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.repository.InsuranceStaffCategoryPeriodRepository;
import com.dtech.admin.repository.RemarkRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.RejectedClaimReportService;
import com.dtech.admin.util.ApprovalRemarkUtil;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.DateTimeUtil;
import com.dtech.admin.util.MedicalClaimStaffCategoryResolver;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class RejectedClaimReportServiceImpl implements RejectedClaimReportService {

    private static final String PAGE_REJECTED_CLAIM_REPORT = WebPage.RPRT_RCR.name();
    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ALL = "ALL";
    private static final String ALL_STAFF_LABEL = "ALL STAFF";
    private static final String ALL_COMPANIES_LABEL = "All Companies";
    private static final String OTHER_REASON = "Other";
    private static final String PERIOD_SEPARATOR = " / ";

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final RemarkRepository remarkRepository;

    @Autowired
    private final InsuranceStaffCategoryPeriodRepository insuranceStaffCategoryPeriodRepository;

    @Autowired
    private final MedicalClaimStaffCategoryResolver staffCategoryResolver;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Rejected claim report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_REJECTED_CLAIM_REPORT);

            responseMap.put("privileges", privileges);
            responseMap.put("company", buildCompanyReference());
            responseMap.put("staffCategories", buildStaffCategoryReference());
            responseMap.put("policyPeriods", buildPolicyPeriodReference());
            responseMap.put("returnReasons", remarkRepository.findAllByRemarkCategoryAndStatus(RemarkCategory.INSURANCE, Status.ACTIVE).stream()
                    .map(remark -> new SimpleBaseDTO(remark.getCode(), remark.getDescription()))
                    .toList());

            auditLogService.log(PAGE_REJECTED_CLAIM_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.REJECTED_CLAIM_REPORT_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load rejected claim report reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<RejectedClaimReportSearchDTO> paginationRequest,
                                                          Locale locale) {
        try {
            log.info("Rejected claim report filter list {}", paginationRequest);
            RejectedClaimReportResponseDTO responseDTO = buildReport(paginationRequest.getSearch());

            auditLogService.log(PAGE_REJECTED_CLAIM_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(responseDTO), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.REJECTED_CLAIM_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter rejected claim report", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<RejectedClaimReportSearchDTO> paginationRequest,
                                         Locale locale) {
        try {
            log.info("Rejected claim report export {}", paginationRequest);
            RejectedClaimReportResponseDTO responseDTO = buildReport(paginationRequest.getSearch());
            byte[] excelBytes = buildExcel(responseDTO);

            auditLogService.log(PAGE_REJECTED_CLAIM_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(paginationRequest.getSearch()), null,
                    paginationRequest.getUsername());

            String fileName = "rejected-claim-report-" + responseDTO.getMonthTitle()
                    .toLowerCase(Locale.ROOT)
                    .replace(" ", "-") + ".xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export rejected claim report", e);
            throw e;
        }
    }

    private RejectedClaimReportResponseDTO buildReport(RejectedClaimReportSearchDTO search) {
        DateRange dateRange = resolveDateRange(search);
        RemarkDictionary remarkDictionary = loadRemarkDictionary();
        Map<String, String> staffCategoryDescriptions = staffCategoryResolver.loadDescriptionMap();
        PeriodSelection selectedPeriod = resolveSelectedPeriod(search);

        List<InsuranceClaimsRequest> claims = insuranceClaimsRequestRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay()).stream()
                .filter(claim -> matchesFilters(claim, search, staffCategoryDescriptions))
                .filter(claim -> matchesSelectedPeriod(claim, selectedPeriod))
                .sorted(Comparator
                        .comparing(this::resolvePeriodSortDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(this::resolveCompanyCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        Map<String, PeriodBucket> periodBuckets = new LinkedHashMap<>();
        for (InsuranceClaimsRequest claim : claims) {
            PeriodInfo periodInfo = resolvePeriodInfo(claim);
            Long bucketPeriodId = selectedPeriod != null ? selectedPeriod.periodId() : periodInfo.periodId();
            String bucketDescription = selectedPeriod != null ? selectedPeriod.description() : periodInfo.description();
            PeriodBucket periodBucket = periodBuckets.computeIfAbsent(periodInfo.key(),
                    key -> new PeriodBucket(bucketPeriodId, bucketDescription));
            CompanyInfo companyInfo = resolveCompanyInfo(claim);
            CompanyBucket bucket = periodBucket.companyBuckets.computeIfAbsent(companyInfo.code(),
                    code -> new CompanyBucket(companyInfo.code(), companyInfo.description()));
            bucket.receivedClaims++;
            periodBucket.totalReceivedClaims++;

            if (isRejectedOrPartiallyRejected(claim)) {
                bucket.rejectedClaims++;
                periodBucket.totalRejectedClaims++;
                List<ReasonAmount> reasons = resolveReturnReasons(claim, remarkDictionary);
                for (ReasonAmount reason : reasons) {
                    bucket.reasonCounts
                            .computeIfAbsent(reason.reason(), key -> new ReasonBucket())
                            .add(reason.amount());
                }
            }
        }

        List<RejectedClaimReportPeriodDTO> periodRows = periodBuckets.values().stream()
                .map(bucket -> new RejectedClaimReportPeriodDTO(
                        bucket.periodId,
                        bucket.periodDescription,
                        bucket.totalReceivedClaims,
                        bucket.totalRejectedClaims,
                        calculatePercentage(bucket.totalRejectedClaims, bucket.totalReceivedClaims),
                        toCompanyRows(bucket.companyBuckets, remarkDictionary)
                ))
                .toList();

        long totalReceived = periodRows.stream().mapToLong(RejectedClaimReportPeriodDTO::getTotalReceivedClaims).sum();
        long totalRejected = periodRows.stream().mapToLong(RejectedClaimReportPeriodDTO::getTotalRejectedClaims).sum();
        List<RejectedClaimReportCompanyDTO> companyRows = aggregateCompanies(periodRows);

        RejectedClaimReportResponseDTO dto = new RejectedClaimReportResponseDTO();
        dto.setTitle("REJECTED CLAIMS REPORT");
        dto.setSubTitle("REASONS OF REJECTED CLAIMS - " + dateRange.monthTitle());
        dto.setStaffCategoryTitle(resolveStaffCategoryTitle(search, staffCategoryDescriptions));
        dto.setPeriod(dateRange.periodText());
        dto.setMonthTitle(dateRange.monthTitle());
        dto.setTotalReceivedClaims(totalReceived);
        dto.setTotalRejectedClaims(totalRejected);
        dto.setRejectedPercentage(calculatePercentage(totalRejected, totalReceived));
        dto.setCompanies(companyRows);
        dto.setPolicyPeriods(periodRows);
        return dto;
    }

    private boolean isRejectedOrPartiallyRejected(InsuranceClaimsRequest claim) {
        if (claim == null) {
            return false;
        }
        if (Workflow.REJECTED.equals(claim.getRequestStatus())) {
            return true;
        }
        return Workflow.APPROVED.equals(claim.getRequestStatus())
                && claim.getRequestAmount() != null
                && claim.getApprovedAmount() != null
                && claim.getRequestAmount().compareTo(claim.getApprovedAmount()) != 0;
    }

    private boolean matchesFilters(InsuranceClaimsRequest claim,
                                   RejectedClaimReportSearchDTO search,
                                   Map<String, String> staffCategoryDescriptions) {
        if (search == null) {
            return true;
        }

        if (hasText(search.getCompany()) && !isAll(search.getCompany())
                && !matchesCompanyFilter(claim, search.getCompany())) {
            return false;
        }

        return !hasText(search.getStaffCategory()) || isAll(search.getStaffCategory())
                || matchesStaffCategoryFilter(claim, search.getStaffCategory(), staffCategoryDescriptions);
    }

    private boolean matchesCompanyFilter(InsuranceClaimsRequest claim, String filter) {
        CompanyInfo company = resolveCompanyInfo(claim);
        String normalizedFilter = normalizeFilterValue(filter);
        return normalizedFilter.equals(normalizeFilterValue(company.code()))
                || normalizedFilter.equals(normalizeFilterValue(company.description()))
                || normalizedFilter.equals(normalizeFilterValue(buildDisplay(company.code(), company.description())));
    }

    private boolean matchesStaffCategoryFilter(InsuranceClaimsRequest claim,
                                               String filter,
                                               Map<String, String> staffCategoryDescriptions) {
        String resolvedCode = staffCategoryResolver.resolveForClaim(claim);
        if (!hasText(resolvedCode)) {
            return false;
        }

        String normalizedCode = staffCategoryResolver.normalizeCode(resolvedCode);
        String description = staffCategoryDescriptions.getOrDefault(normalizedCode, normalizedCode);
        String normalizedFilter = normalizeFilterValue(filter);
        String selectedCode = staffCategoryResolver.normalizeSelectionCode(filter);

        return normalizedFilter.equals(normalizeFilterValue(normalizedCode))
                || normalizedFilter.equals(normalizeFilterValue(description))
                || normalizedFilter.equals(normalizeFilterValue(buildDisplay(normalizedCode, description)))
                || equalsIgnoreCase(selectedCode, resolvedCode);
    }

    private boolean matchesSelectedPeriod(InsuranceClaimsRequest claim, PeriodSelection selectedPeriod) {
        if (selectedPeriod == null) {
            return true;
        }
        InsuranceStaffCategoryPeriod period = resolveClaimPeriod(claim);
        return period != null
                && sameDate(period.getFromDate(), selectedPeriod.fromDate())
                && sameDate(period.getToDate(), selectedPeriod.toDate());
    }

    private PeriodSelection resolveSelectedPeriod(RejectedClaimReportSearchDTO search) {
        if (search == null || !hasText(search.getPeriodId()) || isAll(search.getPeriodId())) {
            return null;
        }
        String filter = search.getPeriodId().trim();
        try {
            Long periodId = Long.valueOf(filter);
            InsuranceStaffCategoryPeriod period = insuranceStaffCategoryPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid policy period"));
            return new PeriodSelection(period.getId(), period.getFromDate(), period.getToDate(), formatPeriod(period));
        } catch (NumberFormatException e) {
            return insuranceStaffCategoryPeriodRepository.findAll().stream()
                    .filter(period -> period != null && period.getFromDate() != null && period.getToDate() != null)
                    .filter(period -> normalizeFilterValue(filter).equals(normalizeFilterValue(formatPeriod(period)))
                            || normalizeFilterValue(filter).equals(normalizeFilterValue(periodKey(period))))
                    .findFirst()
                    .map(period -> new PeriodSelection(period.getId(), period.getFromDate(), period.getToDate(),
                            formatPeriod(period)))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid policy period", e));
        }
    }

    private List<SimpleBaseDTO> buildPolicyPeriodReference() {
        Map<String, SimpleBaseDTO> periods = new LinkedHashMap<>();
        periods.put(ALL, new SimpleBaseDTO(ALL, "All Policy Periods"));

        insuranceStaffCategoryPeriodRepository.findAll().stream()
                .filter(period -> period != null && Status.ACTIVE.equals(period.getStatus()))
                .filter(period -> period.getFromDate() != null && period.getToDate() != null)
                .sorted(Comparator
                        .comparing(InsuranceStaffCategoryPeriod::getFromDate, Comparator.reverseOrder())
                        .thenComparing(InsuranceStaffCategoryPeriod::getToDate, Comparator.reverseOrder()))
                .forEach(period -> periods.putIfAbsent(periodKey(period),
                        new SimpleBaseDTO(String.valueOf(period.getId()), formatPeriod(period))));

        return new ArrayList<>(periods.values());
    }

    private List<RejectedClaimReportCompanyDTO> toCompanyRows(Map<String, CompanyBucket> companyBuckets,
                                                              RemarkDictionary remarkDictionary) {
        return companyBuckets.values().stream()
                .map(bucket -> new RejectedClaimReportCompanyDTO(
                        bucket.companyCode,
                        bucket.companyDescription,
                        bucket.receivedClaims,
                        bucket.rejectedClaims,
                        toReasonRows(bucket.reasonCounts, remarkDictionary)
                ))
                .toList();
    }

    private List<RejectedClaimReportCompanyDTO> aggregateCompanies(List<RejectedClaimReportPeriodDTO> periodRows) {
        Map<String, CompanyBucket> aggregate = new LinkedHashMap<>();
        for (RejectedClaimReportPeriodDTO period : Objects.requireNonNullElse(periodRows, List.<RejectedClaimReportPeriodDTO>of())) {
            for (RejectedClaimReportCompanyDTO company : Objects.requireNonNullElse(period.getCompanies(),
                    List.<RejectedClaimReportCompanyDTO>of())) {
                CompanyBucket bucket = aggregate.computeIfAbsent(company.getCompanyCode(),
                        code -> new CompanyBucket(company.getCompanyCode(), company.getCompanyDescription()));
                bucket.receivedClaims += company.getReceivedClaims();
                bucket.rejectedClaims += company.getRejectedClaims();
                for (RejectedClaimReportReasonDTO reason : Objects.requireNonNullElse(company.getReasons(),
                        List.<RejectedClaimReportReasonDTO>of())) {
                    bucket.reasonCounts
                            .computeIfAbsent(reason.getReturnReason(), key -> new ReasonBucket())
                            .add(reason.getRejectedClaims(), reason.getRejectedAmount());
                }
            }
        }

        return aggregate.values().stream()
                .map(bucket -> new RejectedClaimReportCompanyDTO(
                        bucket.companyCode,
                        bucket.companyDescription,
                        bucket.receivedClaims,
                        bucket.rejectedClaims,
                        bucket.reasonCounts.entrySet().stream()
                                .map(entry -> new RejectedClaimReportReasonDTO(
                                        entry.getValue().count,
                                        entry.getKey(),
                                        entry.getValue().amount))
                                .toList()
                ))
                .toList();
    }

    private List<RejectedClaimReportReasonDTO> toReasonRows(Map<String, ReasonBucket> reasonCounts,
                                                            RemarkDictionary remarkDictionary) {
        return reasonCounts.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, ReasonBucket> entry) -> remarkDictionary.order()
                                .getOrDefault(entry.getKey(), Integer.MAX_VALUE))
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new RejectedClaimReportReasonDTO(
                        entry.getValue().count,
                        entry.getKey(),
                        entry.getValue().amount))
                .toList();
    }

    private BigDecimal calculatePercentage(long rejected, long received) {
        if (received <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(rejected)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(received), 2, RoundingMode.HALF_UP);
    }

    private RemarkDictionary loadRemarkDictionary() {
        Map<String, String> aliases = new HashMap<>();
        Map<String, Integer> order = new LinkedHashMap<>();
        int index = 0;
        for (Remark remark : remarkRepository.findAllByRemarkCategoryAndStatus(RemarkCategory.INSURANCE, Status.ACTIVE)) {
            if (remark == null || !hasText(remark.getDescription())) {
                continue;
            }
            String description = remark.getDescription();
            if (hasText(remark.getCode())) {
                aliases.put(normalizeReason(remark.getCode()), description);
            }
            aliases.put(normalizeReason(description), description);
            order.putIfAbsent(description, index++);
        }
        order.putIfAbsent(OTHER_REASON, Integer.MAX_VALUE);
        return new RemarkDictionary(aliases, order);
    }

    private String resolveReturnReason(String finalRemark, RemarkDictionary remarkDictionary) {
        if (!hasText(finalRemark)) {
            return OTHER_REASON;
        }
        String trimmedRemark = finalRemark.trim();
        String configuredReason = remarkDictionary.aliases().get(normalizeReason(trimmedRemark));
        return configuredReason != null ? configuredReason : trimmedRemark;
    }

    private List<ReasonAmount> resolveReturnReasons(InsuranceClaimsRequest claim, RemarkDictionary remarkDictionary) {
        ApprovalWorkFlow workflow = resolveLatestDisplayWorkflow(claim);
        if (workflow != null && workflow.getRejectReasons() != null && !workflow.getRejectReasons().isEmpty()) {
            return workflow.getRejectReasons().stream()
                    .filter(reason -> reason != null && reason.getAmount() != null)
                    .map(reason -> new ReasonAmount(resolveRejectReasonName(reason, remarkDictionary), reason.getAmount()))
                    .toList();
        }

        String reason = resolveReturnReason(ApprovalRemarkUtil.resolveLevelTwoOrThreeRemark(claim), remarkDictionary);
        return List.of(new ReasonAmount(reason, calculateRejectedAmount(claim)));
    }

    private ApprovalWorkFlow resolveLatestDisplayWorkflow(InsuranceClaimsRequest claim) {
        if (claim == null || claim.getApprovalWorkFlows() == null) {
            return null;
        }
        return claim.getApprovalWorkFlows().stream()
                .filter(workflow -> workflow != null && workflow.getApprovalLevel() != null)
                .filter(workflow -> workflow.getApprovalLevel() == com.dtech.admin.enums.ApprovalLevel.LEVEL02
                        || workflow.getApprovalLevel() == com.dtech.admin.enums.ApprovalLevel.LEVEL03)
                .filter(workflow -> hasText(ApprovalRemarkUtil.resolveWorkflowRemark(workflow)))
                .max(Comparator.comparing(ApprovalWorkFlow::getApprovedDate, Comparator.nullsLast(Date::compareTo)))
                .orElse(null);
    }

    private String resolveRejectReasonName(ApprovalWorkflowRejectReason reason, RemarkDictionary remarkDictionary) {
        if (reason == null) {
            return OTHER_REASON;
        }
        String resolvedReason;
        if (hasText(reason.getReasonDescription())) {
            resolvedReason = reason.getReasonDescription().trim();
        } else if (hasText(reason.getReasonCode())) {
            resolvedReason = resolveReturnReason(reason.getReasonCode(), remarkDictionary);
        } else {
            resolvedReason = OTHER_REASON;
        }
        if (hasText(reason.getRemark())) {
            return resolvedReason + " - " + reason.getRemark().trim();
        }
        return resolvedReason;
    }

    private BigDecimal calculateRejectedAmount(InsuranceClaimsRequest claim) {
        if (claim == null || claim.getRequestAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal approved = claim.getApprovedAmount() != null ? claim.getApprovedAmount() : BigDecimal.ZERO;
        BigDecimal rejected = claim.getRequestAmount().subtract(approved);
        return rejected.compareTo(BigDecimal.ZERO) > 0 ? rejected : BigDecimal.ZERO;
    }

    private List<SimpleBaseDTO> buildCompanyReference() {
        List<SimpleBaseDTO> companies = new ArrayList<>();
        companies.add(new SimpleBaseDTO(ALL, ALL_COMPANIES_LABEL));
        companies.addAll(companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(company -> new SimpleBaseDTO(company.getCode(), company.getDescription()))
                .toList());
        return companies;
    }

    private List<SimpleBaseDTO> buildStaffCategoryReference() {
        List<SimpleBaseDTO> staffCategories = new ArrayList<>();
        staffCategories.add(new SimpleBaseDTO(ALL, ALL_STAFF_LABEL));
        staffCategories.addAll(staffCategoryResolver.loadReferenceCategories());
        return staffCategories;
    }

    private String resolveStaffCategoryTitle(RejectedClaimReportSearchDTO search,
                                             Map<String, String> staffCategoryDescriptions) {
        if (search == null || !hasText(search.getStaffCategory()) || isAll(search.getStaffCategory())) {
            return ALL_STAFF_LABEL;
        }

        String code = staffCategoryResolver.normalizeSelectionCode(search.getStaffCategory());
        String normalizedCode = staffCategoryResolver.normalizeCode(code);
        String description = staffCategoryDescriptions.getOrDefault(normalizedCode, normalizedCode);
        return description + " (" + normalizedCode + ")";
    }

    private CompanyInfo resolveCompanyInfo(InsuranceClaimsRequest claim) {
        CompanyTypes company = Optional.ofNullable(claim)
                .map(InsuranceClaimsRequest::getEmployee)
                .map(employee -> employee.getUserPersonalDetails())
                .map(UserPersonalDetails::getUserCompanyDetails)
                .map(UserCompanyDetails::getCompanyTypes)
                .orElse(null);
        String code = Optional.ofNullable(company).map(CompanyTypes::getCode).filter(this::hasText).orElse("UNKNOWN");
        String description = Optional.ofNullable(company).map(CompanyTypes::getDescription).filter(this::hasText).orElse(code);
        return new CompanyInfo(code, description);
    }

    private String resolveCompanyCode(InsuranceClaimsRequest claim) {
        return resolveCompanyInfo(claim).code();
    }

    private PeriodInfo resolvePeriodInfo(InsuranceClaimsRequest claim) {
        InsuranceStaffCategoryPeriod period = resolveClaimPeriod(claim);
        if (period == null) {
            return new PeriodInfo("UNKNOWN", null, "UNKNOWN");
        }
        return new PeriodInfo(periodKey(period), period.getId(), formatPeriod(period));
    }

    private InsuranceStaffCategoryPeriod resolveClaimPeriod(InsuranceClaimsRequest claim) {
        return Optional.ofNullable(claim)
                .map(InsuranceClaimsRequest::getInsuranceClaimsDetails)
                .map(InsuranceClaimsDetails::getInsuranceStaffCategoryPeriod)
                .orElse(null);
    }

    private Date resolvePeriodSortDate(InsuranceClaimsRequest claim) {
        return Optional.ofNullable(resolveClaimPeriod(claim))
                .map(InsuranceStaffCategoryPeriod::getFromDate)
                .orElse(null);
    }

    private String periodKey(InsuranceStaffCategoryPeriod period) {
        if (period == null || period.getFromDate() == null || period.getToDate() == null) {
            return "UNKNOWN";
        }
        return formatDate(period.getFromDate()) + "|" + formatDate(period.getToDate());
    }

    private String formatPeriod(InsuranceStaffCategoryPeriod period) {
        if (period == null || period.getFromDate() == null || period.getToDate() == null) {
            return "UNKNOWN";
        }
        return formatDate(period.getFromDate()) + PERIOD_SEPARATOR + formatDate(period.getToDate());
    }

    private boolean sameDate(Date first, Date second) {
        return first != null && second != null && formatDate(first).equals(formatDate(second));
    }

    private DateRange resolveDateRange(RejectedClaimReportSearchDTO search) {
        if (search == null || !hasText(search.getFromDate()) || !hasText(search.getToDate())) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        try {
            Date start = DateTimeUtil.getStartOfDay(normalizeDate(search.getFromDate()));
            Date end = DateTimeUtil.getEndOfDay(normalizeDate(search.getToDate()));
            if (start.after(end)) {
                throw new IllegalArgumentException("fromDate must be before or equal to toDate");
            }
            return new DateRange(start, end);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date range", e);
        }
    }

    private byte[] buildExcel(RejectedClaimReportResponseDTO report) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Rejected Claim Report");
            sheet.setDefaultRowHeightInPoints(20);

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subTitleStyle = createSubTitleStyle(workbook);
            CellStyle staffHeaderStyle = createStaffHeaderStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook, false);
            CellStyle totalStyle = createBodyStyle(workbook, true);
            CellStyle percentageStyle = createPercentageStyle(workbook);

            int rowIndex = 1;
            writeMergedCell(sheet, rowIndex++, 0, 3, report.getTitle(), titleStyle);
            rowIndex++;
            writeMergedCell(sheet, rowIndex++, 0, 3, report.getSubTitle(), subTitleStyle);
            writeMergedCell(sheet, rowIndex++, 0, 3, report.getStaffCategoryTitle(), staffHeaderStyle);

            for (RejectedClaimReportPeriodDTO period : Objects.requireNonNullElse(report.getPolicyPeriods(),
                    List.<RejectedClaimReportPeriodDTO>of())) {
                writeMergedCell(sheet, rowIndex++, 0, 4,
                        "POLICY PERIOD: " + Objects.requireNonNullElse(period.getPeriodDescription(), "UNKNOWN"),
                        staffHeaderStyle);
                rowIndex = writeCompanyTable(sheet, rowIndex, period.getCompanies(), headerStyle, bodyStyle, totalStyle);
                rowIndex++;
            }

            rowIndex++;
            Row percentageRow = sheet.createRow(rowIndex);
            writeTextCell(percentageRow, 0, "Percentage", percentageStyle);
            writeTextCell(percentageRow, 1, formatPercentage(report.getRejectedPercentage()), percentageStyle);
            writeTextCell(percentageRow, 2, "", percentageStyle);
            writeTextCell(percentageRow, 3, "", percentageStyle);
            writeTextCell(percentageRow, 4, "", percentageStyle);

            int[] widths = {20, 22, 24, 20, 55};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build rejected claim report Excel", e);
        }
    }

    private int writeCompanyTable(Sheet sheet,
                                  int rowIndex,
                                  List<RejectedClaimReportCompanyDTO> companies,
                                  CellStyle headerStyle,
                                  CellStyle bodyStyle,
                                  CellStyle totalStyle) {
        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = {"COMPANY", "NO. OF RECEIVED", "NO. OF REJECTED CLAIMS", "REJECTED AMOUNT", "RETURN REASON"};
        for (int i = 0; i < headers.length; i++) {
            writeTextCell(headerRow, i, headers[i], headerStyle);
        }

        for (RejectedClaimReportCompanyDTO company : Objects.requireNonNullElse(companies,
                List.<RejectedClaimReportCompanyDTO>of())) {
            List<RejectedClaimReportReasonDTO> reasons = Objects.requireNonNullElse(company.getReasons(),
                    List.<RejectedClaimReportReasonDTO>of());

            if (reasons.isEmpty()) {
                Row row = sheet.createRow(rowIndex++);
                writeTextCell(row, 0, company.getCompanyCode(), bodyStyle);
                writeNumberCell(row, 1, company.getReceivedClaims(), bodyStyle);
                writeNumberCell(row, 2, 0L, bodyStyle);
                writeAmountCell(row, 3, BigDecimal.ZERO, bodyStyle);
                writeTextCell(row, 4, "", bodyStyle);
                continue;
            }

            boolean firstReason = true;
            for (RejectedClaimReportReasonDTO reason : reasons) {
                Row row = sheet.createRow(rowIndex++);
                writeTextCell(row, 0, firstReason ? company.getCompanyCode() : "", bodyStyle);
                writeNumberCell(row, 1, firstReason ? company.getReceivedClaims() : null, bodyStyle);
                writeNumberCell(row, 2, reason.getRejectedClaims(), bodyStyle);
                writeAmountCell(row, 3, reason.getRejectedAmount(), bodyStyle);
                writeTextCell(row, 4, reason.getReturnReason(), bodyStyle);
                firstReason = false;
            }

            Row totalRow = sheet.createRow(rowIndex++);
            writeTextCell(totalRow, 0, "", totalStyle);
            writeTextCell(totalRow, 1, "", totalStyle);
            writeNumberCell(totalRow, 2, company.getRejectedClaims(), totalStyle);
            writeAmountCell(totalRow, 3, reasons.stream()
                    .map(RejectedClaimReportReasonDTO::getRejectedAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add), totalStyle);
            writeTextCell(totalRow, 4, "", totalStyle);
            rowIndex++;
        }

        return rowIndex;
    }

    private void writeMergedCell(Sheet sheet, int rowIndex, int firstColumn, int lastColumn, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(25);
        for (int column = firstColumn; column <= lastColumn; column++) {
            Cell cell = row.createCell(column);
            cell.setCellStyle(style);
            if (column == firstColumn) {
                cell.setCellValue(value != null ? value : "");
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstColumn, lastColumn));
    }

    private void writeTextCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void writeNumberCell(Row row, int index, Long value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void writeAmountCell(Row row, int index, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setUnderline(Font.U_SINGLE);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubTitleStyle(Workbook workbook) {
        CellStyle style = createTitleStyle(workbook);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createStaffHeaderStyle(Workbook workbook) {
        CellStyle style = createBorderedStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = createBorderedStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook, boolean bold) {
        CellStyle style = createBorderedStyle(workbook);
        if (bold) {
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
        }
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createPercentageStyle(Workbook workbook) {
        CellStyle style = createBorderedStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createBorderedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String formatPercentage(BigDecimal percentage) {
        BigDecimal safePercentage = percentage != null ? percentage : BigDecimal.ZERO;
        return new DecimalFormat("0.##").format(safePercentage) + "%";
    }

    private String normalizeDate(String value) {
        return value.contains("-") ? value.replace("-", "/") : value;
    }

    private static String formatDate(Date date) {
        return java.time.Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString();
    }

    private String normalizeReason(String value) {
        return value == null ? "" : value.trim()
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String buildDisplay(String code, String description) {
        if (!hasText(code)) {
            return description != null ? description : "";
        }
        if (!hasText(description)) {
            return code;
        }
        return code + " - " + description;
    }

    private String normalizeFilterValue(String value) {
        return value == null
                ? ""
                : value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private boolean isAll(String value) {
        return ALL.equalsIgnoreCase(value != null ? value.trim() : null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean equalsIgnoreCase(String expected, String actual) {
        return expected != null && actual != null && expected.trim().equalsIgnoreCase(actual.trim());
    }

    private record DateRange(Date startOfDay, Date endOfDay) {
        private String periodText() {
            return formatDate(startOfDay) + PERIOD_SEPARATOR + formatDate(endOfDay);
        }

        private String monthTitle() {
            java.time.LocalDate localDate = startOfDay.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            String month = Month.of(localDate.getMonthValue()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            return month.toUpperCase(Locale.ROOT) + " " + localDate.getYear();
        }

        private static String formatDate(Date date) {
            return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString();
        }
    }

    private record CompanyInfo(String code, String description) {
    }

    private record PeriodSelection(Long periodId, Date fromDate, Date toDate, String description) {
    }

    private record PeriodInfo(String key, Long periodId, String description) {
    }

    private record RemarkDictionary(Map<String, String> aliases, Map<String, Integer> order) {
    }

    private record ReasonAmount(String reason, BigDecimal amount) {
    }

    private static class PeriodBucket {
        private final Long periodId;
        private final String periodDescription;
        private long totalReceivedClaims;
        private long totalRejectedClaims;
        private final Map<String, CompanyBucket> companyBuckets = new LinkedHashMap<>();

        private PeriodBucket(Long periodId, String periodDescription) {
            this.periodId = periodId;
            this.periodDescription = periodDescription;
        }
    }

    private static class CompanyBucket {
        private final String companyCode;
        private final String companyDescription;
        private long receivedClaims;
        private long rejectedClaims;
        private final Map<String, ReasonBucket> reasonCounts = new LinkedHashMap<>();

        private CompanyBucket(String companyCode, String companyDescription) {
            this.companyCode = companyCode;
            this.companyDescription = companyDescription;
        }
    }

    private static class ReasonBucket {
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;

        private void add(BigDecimal rejectedAmount) {
            add(1L, rejectedAmount);
        }

        private void add(long rejectedClaims, BigDecimal rejectedAmount) {
            this.count += rejectedClaims;
            if (rejectedAmount != null) {
                this.amount = this.amount.add(rejectedAmount);
            }
        }
    }
}

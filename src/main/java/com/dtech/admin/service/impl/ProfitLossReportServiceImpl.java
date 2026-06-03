package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.ProfitLossReportRowDTO;
import com.dtech.admin.dto.search.ChequePaymentSearchDTO;
import com.dtech.admin.dto.search.ProfitLossReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.PaymentAdviceStatus;
import com.dtech.admin.enums.PaymentAdviceType;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ChequePayment;
import com.dtech.admin.model.ChequePaymentDdf;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.model.PaymentAdviceAttachment;
import com.dtech.admin.model.PaymentAttachmentClaim;
import com.dtech.admin.repository.ChequePaymentDdfRepository;
import com.dtech.admin.repository.ChequePaymentRepository;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.PaymentAdviceAttachmentRepository;
import com.dtech.admin.repository.PaymentAdviceRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.ProfitLossReportService;
import com.dtech.admin.specifications.ChequePaymentDdfSpecification;
import com.dtech.admin.specifications.ChequePaymentSpecification;
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
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Log4j2
@RequiredArgsConstructor
public class ProfitLossReportServiceImpl implements ProfitLossReportService {

    private static final String PAGE_PROFIT_LOSS_REPORT = WebPage.RPRT_PNL.name();

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final PaymentAdviceRepository paymentAdviceRepository;

    @Autowired
    private final PaymentAdviceAttachmentRepository paymentAdviceAttachmentRepository;

    @Autowired
    private final ChequePaymentRepository chequePaymentRepository;

    @Autowired
    private final ChequePaymentDdfRepository chequePaymentDdfRepository;

    @Autowired
    private final MedicalClaimStaffCategoryResolver medicalClaimStaffCategoryResolver;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Profit and loss report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new LinkedHashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_PROFIT_LOSS_REPORT);

            List<SimpleBaseDTO> company = companyTypeRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();
            List<SimpleBaseDTO> staffCategories = medicalClaimStaffCategoryResolver.loadReferenceCategories();

            responseMap.put("privileges", privileges);
            responseMap.put("company", company);
            responseMap.put("staffCategories", staffCategories);
            responseMap.put("months", buildMonthList());
            responseMap.put("years", buildYearList());
            responseMap.put("reportType", buildReportTypeList());

            auditLogService.log(PAGE_PROFIT_LOSS_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.PROFIT_LOSS_REPORT_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load profit and loss report reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ProfitLossReportSearchDTO> paginationRequest,
                                                          Locale locale) {
        try {
            log.info("Profit and loss report filter list {}", paginationRequest);
            List<ProfitLossReportRowDTO> rows = resolveRows(paginationRequest.getSearch());
            List<ProfitLossReportRowDTO> sortedRows = sortRows(rows, paginationRequest);
            PagingResult<ProfitLossReportRowDTO> result = buildPagingResult(sortedRows, paginationRequest);

            auditLogService.log(PAGE_PROFIT_LOSS_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(result.getContent()), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.PROFIT_LOSS_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter profit and loss report", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<ProfitLossReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Profit and loss report export {}", paginationRequest);
            List<ProfitLossReportRowDTO> rows = resolveRows(paginationRequest.getSearch());
            rows = sortRows(rows, paginationRequest);

            byte[] excelBytes = buildExcel(rows);
            auditLogService.log(PAGE_PROFIT_LOSS_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(paginationRequest.getSearch()), null,
                    paginationRequest.getUsername());

            String fileName = "profit-loss-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export profit and loss report", e);
            throw e;
        }
    }

    private List<ProfitLossReportRowDTO> resolveRows(ProfitLossReportSearchDTO search) {
        ProfitLossReportSearchDTO filter = search != null ? search : new ProfitLossReportSearchDTO();
        ReportType reportType = ReportType.from(filter.getReportType());
        Set<String> monthFilter = normalizeMonths(filter.getMonths());
        String companyFilter = hasText(filter.getCompany()) ? filter.getCompany().trim() : null;
        Integer yearFilter = parseYear(filter.getYear());
        String staffCategoryFilter = hasText(filter.getStaffCategory())
                ? medicalClaimStaffCategoryResolver.normalizeSelectionCode(filter.getStaffCategory())
                : null;
        List<String> storedStaffCategoryCodes = hasText(staffCategoryFilter)
                ? medicalClaimStaffCategoryResolver.expandStoredCodesForFilter(staffCategoryFilter)
                : List.of();

        Map<String, String> companyDescriptions = loadCompanyDescriptions();
        Map<String, String> staffCategoryDescriptions = reportType == ReportType.HEALTH_CLAIM && hasText(staffCategoryFilter)
                ? medicalClaimStaffCategoryResolver.loadDescriptionMap()
                : Map.of();
        Map<String, ProfitLossReportRowDTO> summary = new LinkedHashMap<>();

        if (reportType.includesMedical()) {
            List<PaymentAdvice> advices = loadPaymentAdvice(PaymentAdviceType.MEDICAL, true, companyFilter,
                    storedStaffCategoryCodes, yearFilter);
            addPaidAmounts(summary, advices, monthFilter, companyDescriptions);

            List<ChequePayment> cheques = loadChequePayments(companyFilter, storedStaffCategoryCodes, monthFilter, yearFilter);
            addReceivedAmounts(summary, cheques, monthFilter, companyDescriptions);
        }

        if (reportType.includesDdf()) {
            List<PaymentAdvice> advices = loadPaymentAdvice(PaymentAdviceType.DEATH, false, companyFilter,
                    null, yearFilter);
            addPaidAmounts(summary, advices, monthFilter, companyDescriptions);

            List<ChequePaymentDdf> cheques = loadChequePaymentsDdf(companyFilter, monthFilter, yearFilter);
            addReceivedAmountsDdf(summary, cheques, monthFilter, companyDescriptions);
        }

        List<ProfitLossReportRowDTO> rows = new ArrayList<>(summary.values());
        for (ProfitLossReportRowDTO row : rows) {
            BigDecimal totalPaid = safeAmount(row.getTotalPaid());
            BigDecimal totalReceived = safeAmount(row.getTotalReceived());
            BigDecimal difference = totalReceived.subtract(totalPaid);
            row.setTotalPaid(totalPaid);
            row.setTotalReceived(totalReceived);
            row.setDifference(difference);
            row.setResult(resolveResult(difference));
            row.setResultDescription(resolveResultDescription(difference));
            if (reportType == ReportType.HEALTH_CLAIM && hasText(staffCategoryFilter)) {
                row.setStaffCategoryCode(staffCategoryFilter);
                row.setStaffCategoryDescription(staffCategoryDescriptions.get(staffCategoryFilter));
            }
        }

        return rows;
    }

    private void addPaidAmounts(Map<String, ProfitLossReportRowDTO> summary,
                                List<PaymentAdvice> advices,
                                Set<String> monthFilter,
                                Map<String, String> companyDescriptions) {
        Map<Long, List<PaymentAdviceAttachment>> attachmentsByAdviceId = loadAdviceAttachmentsByAdviceId(advices);
        for (PaymentAdvice advice : advices) {
            if (advice == null) {
                continue;
            }
            String year = resolveAdviceYear(advice);
            if (!hasText(year)) {
                continue;
            }
            if (!matchesMonth(advice.getCreatedDate(), monthFilter)) {
                continue;
            }
            String companyCode = advice.getCompanyCode();
            if (!hasText(companyCode)) {
                continue;
            }
            String key = companyCode + "|" + year;
            ProfitLossReportRowDTO row = summary.computeIfAbsent(key, code ->
                    buildRow(companyCode, companyDescriptions.get(companyCode), year));
            BigDecimal amount = resolvePaidAmount(advice, attachmentsByAdviceId.get(advice.getId()));
            row.setTotalPaid(safeAmount(row.getTotalPaid()).add(amount));
        }
    }

    private void addReceivedAmounts(Map<String, ProfitLossReportRowDTO> summary,
                                    List<ChequePayment> cheques,
                                    Set<String> monthFilter,
                                    Map<String, String> companyDescriptions) {
        for (ChequePayment payment : cheques) {
            if (payment == null) {
                continue;
            }
            String year = resolveChequeYear(payment.getYear(), payment.getChequeDate());
            if (!hasText(year)) {
                continue;
            }
            String companyCode = payment.getCompanyCode();
            if (!hasText(companyCode)) {
                continue;
            }
            BigDecimal received = calculateReceivedAmount(payment.getMonths(), payment.getAmount(), monthFilter);
            if (received.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            String key = companyCode + "|" + year;
            ProfitLossReportRowDTO row = summary.computeIfAbsent(key, code ->
                    buildRow(companyCode, companyDescriptions.get(companyCode), year));
            row.setTotalReceived(safeAmount(row.getTotalReceived()).add(received));
        }
    }

    private void addReceivedAmountsDdf(Map<String, ProfitLossReportRowDTO> summary,
                                       List<ChequePaymentDdf> cheques,
                                       Set<String> monthFilter,
                                       Map<String, String> companyDescriptions) {
        for (ChequePaymentDdf payment : cheques) {
            if (payment == null) {
                continue;
            }
            String year = resolveChequeYear(payment.getYear(), payment.getChequeDate());
            if (!hasText(year)) {
                continue;
            }
            String companyCode = payment.getCompanyCode();
            if (!hasText(companyCode)) {
                continue;
            }
            BigDecimal received = calculateReceivedAmount(payment.getMonths(), payment.getAmount(), monthFilter);
            if (received.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            String key = companyCode + "|" + year;
            ProfitLossReportRowDTO row = summary.computeIfAbsent(key, code ->
                    buildRow(companyCode, companyDescriptions.get(companyCode), year));
            row.setTotalReceived(safeAmount(row.getTotalReceived()).add(received));
        }
    }

    private List<PaymentAdvice> loadPaymentAdvice(PaymentAdviceType type,
                                                  boolean includeNullType,
                                                  String companyCode,
                                                  List<String> staffCategoryCodes,
                                                  Integer yearFilter) {
        Specification<PaymentAdvice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (includeNullType) {
                predicates.add(cb.or(cb.isNull(root.get("type")), cb.equal(root.get("type"), type)));
            } else {
                predicates.add(cb.equal(root.get("type"), type));
            }
            predicates.add(cb.equal(root.get("status"), PaymentAdviceStatus.FINALIZED));
            if (hasText(companyCode)) {
                predicates.add(cb.equal(cb.lower(root.get("companyCode")), companyCode.toLowerCase()));
            }
            if (staffCategoryCodes != null && !staffCategoryCodes.isEmpty()) {
                predicates.add(cb.lower(root.get("staffCategoryCode")).in(
                        staffCategoryCodes.stream().map(String::toLowerCase).toList()));
            }
            if (yearFilter != null) {
                Date startDate = buildYearStart(yearFilter);
                Date endDate = buildYearEnd(yearFilter);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), startDate));
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return paymentAdviceRepository.findAll(spec);
    }

    private List<ChequePayment> loadChequePayments(String companyCode,
                                                   List<String> staffCategoryCodes,
                                                   Set<String> monthFilter,
                                                   Integer yearFilter) {
        ChequePaymentSearchDTO search = new ChequePaymentSearchDTO();
        if (hasText(companyCode)) {
            search.setCompany(companyCode);
        }
        if (staffCategoryCodes != null && !staffCategoryCodes.isEmpty()) {
            search.setStaffCategoryCodes(staffCategoryCodes);
        }
        if (yearFilter != null) {
            search.setYear(String.valueOf(yearFilter));
        }
        if (!monthFilter.isEmpty()) {
            search.setMonths(new ArrayList<>(monthFilter));
        }
        return chequePaymentRepository.findAll(ChequePaymentSpecification.getSpecification(search));
    }

    private List<ChequePaymentDdf> loadChequePaymentsDdf(String companyCode, Set<String> monthFilter, Integer yearFilter) {
        ChequePaymentSearchDTO search = new ChequePaymentSearchDTO();
        if (hasText(companyCode)) {
            search.setCompany(companyCode);
        }
        if (yearFilter != null) {
            search.setYear(String.valueOf(yearFilter));
        }
        if (!monthFilter.isEmpty()) {
            search.setMonths(new ArrayList<>(monthFilter));
        }
        return chequePaymentDdfRepository.findAll(ChequePaymentDdfSpecification.getSpecification(search));
    }

    private BigDecimal calculateReceivedAmount(List<String> chequeMonths,
                                               BigDecimal amount,
                                               Set<String> monthFilter) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        List<String> normalizedMonths = normalizeMonthList(chequeMonths);
        if (normalizedMonths.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<String> chequeMonthSet = new HashSet<>(normalizedMonths);
        int totalMonths = chequeMonthSet.size();
        if (totalMonths == 0) {
            return BigDecimal.ZERO;
        }

        int matched = 0;
        if (monthFilter.isEmpty()) {
            matched = totalMonths;
        } else {
            for (String month : chequeMonthSet) {
                if (monthFilter.contains(month)) {
                    matched++;
                }
            }
        }
        if (matched == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal perMonth = amount.divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
        return perMonth.multiply(BigDecimal.valueOf(matched));
    }

    private ProfitLossReportRowDTO buildRow(String companyCode, String companyDescription, String year) {
        ProfitLossReportRowDTO row = new ProfitLossReportRowDTO();
        row.setCompanyCode(companyCode);
        row.setCompanyDescription(companyDescription != null ? companyDescription : "");
        row.setYear(year);
        row.setTotalPaid(BigDecimal.ZERO);
        row.setTotalReceived(BigDecimal.ZERO);
        row.setDifference(BigDecimal.ZERO);
        return row;
    }

    private Map<Long, List<PaymentAdviceAttachment>> loadAdviceAttachmentsByAdviceId(List<PaymentAdvice> advices) {
        if (advices == null || advices.isEmpty()) {
            return Map.of();
        }
        List<PaymentAdvice> persistedAdvices = advices.stream()
                .filter(Objects::nonNull)
                .filter(advice -> advice.getId() != null)
                .toList();
        if (persistedAdvices.isEmpty()) {
            return Map.of();
        }
        return paymentAdviceAttachmentRepository.findAllByPaymentAdviceIn(persistedAdvices)
                .stream()
                .filter(Objects::nonNull)
                .filter(attachment -> attachment.getPaymentAdvice() != null)
                .filter(attachment -> attachment.getPaymentAdvice().getId() != null)
                .collect(Collectors.groupingBy(attachment -> attachment.getPaymentAdvice().getId()));
    }

    private BigDecimal resolvePaidAmount(PaymentAdvice advice, List<PaymentAdviceAttachment> adviceAttachments) {
        if (isMedicalAdvice(advice)) {
            return resolveMedicalPaidAmountFromClaims(adviceAttachments);
        }
        BigDecimal approved = advice.getTotalApprovedAmount();
        if (approved != null) {
            return approved;
        }
        BigDecimal requested = advice.getTotalRequestedAmount();
        return requested != null ? requested : BigDecimal.ZERO;
    }

    private BigDecimal resolveMedicalPaidAmountFromClaims(List<PaymentAdviceAttachment> adviceAttachments) {
        if (adviceAttachments == null || adviceAttachments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> claimIds = new HashSet<>();
        for (PaymentAdviceAttachment adviceAttachment : adviceAttachments) {
            if (adviceAttachment == null || adviceAttachment.getPaymentAttachment() == null
                    || adviceAttachment.getPaymentAttachment().getClaims() == null) {
                continue;
            }
            for (PaymentAttachmentClaim attachmentClaim : adviceAttachment.getPaymentAttachment().getClaims()) {
                if (attachmentClaim == null) {
                    continue;
                }
                InsuranceClaimsRequest claim = attachmentClaim.getInsuranceClaimsRequest();
                if (claim == null || claim.getId() == null || !claimIds.add(claim.getId())) {
                    continue;
                }
                if (!Workflow.APPROVED.equals(claim.getRequestStatus())) {
                    continue;
                }
                BigDecimal approvedAmount = claim.getApprovedAmount() != null
                        ? claim.getApprovedAmount()
                        : attachmentClaim.getApprovedAmount();
                total = total.add(safeAmount(approvedAmount));
            }
        }
        return total;
    }

    private boolean isMedicalAdvice(PaymentAdvice advice) {
        return advice != null && (advice.getType() == null || PaymentAdviceType.MEDICAL.equals(advice.getType()));
    }

    private String resolveResult(BigDecimal difference) {
        int compare = difference.compareTo(BigDecimal.ZERO);
        if (compare > 0) {
            return "PROFIT";
        }
        if (compare < 0) {
            return "LOSS";
        }
        return "BREAKEVEN";
    }

    private String resolveResultDescription(BigDecimal difference) {
        int compare = difference.compareTo(BigDecimal.ZERO);
        if (compare > 0) {
            return "Profit";
        }
        if (compare < 0) {
            return "Loss";
        }
        return "Breakeven";
    }

    private boolean matchesMonth(java.util.Date createdDate, Set<String> monthFilter) {
        if (monthFilter.isEmpty()) {
            return true;
        }
        if (createdDate == null) {
            return false;
        }
        int monthValue = DateTimeUtil.getMonth(createdDate);
        String normalized = String.format("%02d", monthValue);
        return monthFilter.contains(normalized);
    }

    private Set<String> normalizeMonths(List<String> months) {
        if (months == null || months.isEmpty()) {
            return Set.of();
        }
        return months.stream()
                .filter(this::hasText)
                .map(String::trim)
                .map(this::normalizeMonth)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<String> normalizeMonthList(List<String> months) {
        if (months == null || months.isEmpty()) {
            return List.of();
        }
        return months.stream()
                .filter(this::hasText)
                .map(String::trim)
                .map(this::normalizeMonth)
                .filter(Objects::nonNull)
                .toList();
    }

    private String normalizeMonth(String value) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            int month = Integer.parseInt(trimmed);
            if (month < 1 || month > 12) {
                return null;
            }
            return String.format("%02d", month);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveAdviceYear(PaymentAdvice advice) {
        if (advice == null || advice.getCreatedDate() == null) {
            return null;
        }
        return String.valueOf(DateTimeUtil.getYear(advice.getCreatedDate()));
    }

    private String resolveChequeYear(String chequeYear, java.util.Date chequeDate) {
        if (hasText(chequeYear)) {
            return chequeYear.trim();
        }
        if (chequeDate != null) {
            return String.valueOf(DateTimeUtil.getYear(chequeDate));
        }
        return null;
    }

    private Integer parseYear(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Date buildYearStart(int year) {
        LocalDate date = LocalDate.of(year, 1, 1);
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date buildYearEnd(int year) {
        LocalDate date = LocalDate.of(year, 12, 31);
        return Date.from(date.atTime(LocalTime.of(23, 59, 59)).atZone(ZoneId.systemDefault()).toInstant());
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private List<SimpleBaseDTO> buildMonthList() {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(Month::of)
                .map(month -> new SimpleBaseDTO(String.format("%02d", month.getValue()), month.name()))
                .collect(Collectors.toList());
    }

    private List<SimpleBaseDTO> buildYearList() {
        int currentYear = DateTimeUtil.getCurrentYear();
        return IntStream.rangeClosed(currentYear - 5, currentYear)
                .mapToObj(String::valueOf)
                .sorted(Comparator.reverseOrder())
                .map(val -> new SimpleBaseDTO(val, val))
                .collect(Collectors.toList());
    }

    private List<SimpleBaseDTO> buildReportTypeList() {
        List<SimpleBaseDTO> types = new ArrayList<>();
        types.add(new SimpleBaseDTO("HEALTH_CLAIM", "Health Claim"));
        types.add(new SimpleBaseDTO("DDF", "DDF"));
        return types;
    }

    private Map<String, String> loadCompanyDescriptions() {
        return companyTypeRepository.findAll().stream()
                .collect(Collectors.toMap(CompanyTypes::getCode, CompanyTypes::getDescription, (a, b) -> a));
    }

    private Map<String, String> loadStaffCategoryDescriptions() {
        return medicalClaimStaffCategoryResolver.loadDescriptionMap();
    }

    private List<ProfitLossReportRowDTO> sortRows(List<ProfitLossReportRowDTO> rows,
                                                  PaginationRequest<ProfitLossReportSearchDTO> paginationRequest) {
        String sortColumn = resolveSortColumn(paginationRequest.getSortColumn());
        if (sortColumn == null) {
            return rows;
        }

        Comparator<ProfitLossReportRowDTO> comparator = switch (sortColumn) {
            case "companyCode" -> Comparator.comparing(ProfitLossReportRowDTO::getCompanyCode,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "year" -> Comparator.comparing(ProfitLossReportRowDTO::getYear,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "totalPaid" -> Comparator.comparing(ProfitLossReportRowDTO::getTotalPaid,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "totalReceived" -> Comparator.comparing(ProfitLossReportRowDTO::getTotalReceived,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "difference" -> Comparator.comparing(ProfitLossReportRowDTO::getDifference,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "result" -> Comparator.comparing(ProfitLossReportRowDTO::getResult,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            default -> null;
        };

        if (comparator == null) {
            return rows;
        }

        if (paginationRequest.getSortDirection() == Sort.Direction.DESC) {
            comparator = comparator.reversed();
        }

        List<ProfitLossReportRowDTO> sortedRows = new ArrayList<>(rows);
        sortedRows.sort(comparator);
        return sortedRows;
    }

    private String resolveSortColumn(String sortColumn) {
        if (sortColumn == null || sortColumn.isBlank()) {
            return null;
        }
        return switch (sortColumn) {
            case "company", "companyCode" -> "companyCode";
            case "year" -> "year";
            case "totalPaid" -> "totalPaid";
            case "totalReceived" -> "totalReceived";
            case "difference" -> "difference";
            case "result" -> "result";
            default -> null;
        };
    }

    private PagingResult<ProfitLossReportRowDTO> buildPagingResult(List<ProfitLossReportRowDTO> rows,
                                                                   PaginationRequest<ProfitLossReportSearchDTO> paginationRequest) {
        int page = paginationRequest.getPage() != null ? paginationRequest.getPage() : 0;
        int size = paginationRequest.getSize() != null ? paginationRequest.getSize() : rows.size();
        int fromIndex = Math.min(page * size, rows.size());
        int toIndex = Math.min(fromIndex + size, rows.size());
        List<ProfitLossReportRowDTO> content = rows.subList(fromIndex, toIndex);
        return new PagingResult<>(content, content.size(), rows.size());
    }

    private byte[] buildExcel(List<ProfitLossReportRowDTO> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Profit and Loss");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

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

            sheet.setColumnWidth(0, 6 * 256);
            sheet.setColumnWidth(1, 28 * 256);
            sheet.setColumnWidth(2, 10 * 256);
            sheet.setColumnWidth(3, 18 * 256);
            sheet.setColumnWidth(4, 18 * 256);
            sheet.setColumnWidth(5, 18 * 256);
            sheet.setColumnWidth(6, 12 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("Profit and Loss Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Company", headerStyle);
            createStringCell(row, 2, "Year", headerStyle);
            createStringCell(row, 3, "Total Paid", headerStyle);
            createStringCell(row, 4, "Total Received", headerStyle);
            createStringCell(row, 5, "Difference", headerStyle);
            createStringCell(row, 6, "Result", headerStyle);

            int lineNo = 1;
            for (ProfitLossReportRowDTO rowDTO : rows) {
                row = sheet.createRow(rowIndex++);
                String companyDisplay = buildDisplay(rowDTO.getCompanyCode(), rowDTO.getCompanyDescription());
                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, companyDisplay, dataStyle);
                createStringCell(row, 2, rowDTO.getYear(), dataStyle);
                createStringCell(row, 3, toAmountString(rowDTO.getTotalPaid()), dataStyle);
                createStringCell(row, 4, toAmountString(rowDTO.getTotalReceived()), dataStyle);
                createStringCell(row, 5, toAmountString(rowDTO.getDifference()), dataStyle);
                createStringCell(row, 6, rowDTO.getResultDescription(), dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate profit and loss report excel", e);
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

    private String buildDisplay(String code, String description) {
        if (!hasText(code)) {
            return "";
        }
        if (!hasText(description)) {
            return code;
        }
        return code + " - " + description;
    }

    private String toAmountString(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "0";
    }

    private enum ReportType {
        HEALTH_CLAIM,
        DDF,
        BOTH;

        static ReportType from(String value) {
            if (value == null || value.isBlank()) {
                return BOTH;
            }
            String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
            return switch (normalized) {
                case "HEALTHCLAIM", "HEALTH_CLAIM", "HEALTH", "MEDICAL" -> HEALTH_CLAIM;
                case "DDF", "DEATH" -> DDF;
                case "BOTH", "ALL" -> BOTH;
                default -> BOTH;
            };
        }

        boolean includesMedical() {
            return this == HEALTH_CLAIM || this == BOTH;
        }

        boolean includesDdf() {
            return this == DDF || this == BOTH;
        }
    }
}

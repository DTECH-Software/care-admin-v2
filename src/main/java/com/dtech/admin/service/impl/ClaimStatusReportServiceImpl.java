package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.ClaimStatusReportRowDTO;
import com.dtech.admin.dto.search.ClaimStatusReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.DependentCategory;
import com.dtech.admin.enums.RelationCategory;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.Treatment;
import com.dtech.admin.model.TreatmentCategory;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.repository.TreatmentCategoryRepository;
import com.dtech.admin.repository.TreatmentRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.ClaimStatusReportService;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClaimStatusReportServiceImpl implements ClaimStatusReportService {

    private static final String PAGE_CLAIM_STATUS_REPORT = WebPage.RPRT_CSR.name();
    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String SELF = "SELF";

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
    private final TreatmentRepository treatmentRepository;

    @Autowired
    private final TreatmentCategoryRepository treatmentCategoryRepository;

    @Autowired
    private final MedicalClaimStaffCategoryResolver staffCategoryResolver;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Claim status report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_CLAIM_STATUS_REPORT);

            responseMap.put("privileges", privileges);
            responseMap.put("claimStatus", Arrays.stream(Workflow.values())
                    .map(status -> new SimpleBaseDTO(status.name(), status.getDescription()))
                    .toList());
            responseMap.put("company", companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                    .map(company -> new SimpleBaseDTO(company.getCode(), company.getDescription()))
                    .toList());
            responseMap.put("staffCategories", staffCategoryResolver.loadReferenceCategories());
            responseMap.put("dependentCategory", buildDependentCategoryReference());
            responseMap.put("treatment", treatmentRepository.findAllByStatus(Status.ACTIVE).stream()
                    .map(treatment -> new SimpleBaseDTO(treatment.getTreatmentCode(), treatment.getTreatmentDescription()))
                    .toList());
            responseMap.put("treatmentCategory", treatmentCategoryRepository.findAllByStatus(Status.ACTIVE).stream()
                    .map(category -> new SimpleBaseDTO(category.getCode(), category.getDescription()))
                    .toList());

            auditLogService.log(PAGE_CLAIM_STATUS_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.CLAIM_STATUS_REPORT_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load claim status report reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ClaimStatusReportSearchDTO> paginationRequest,
                                                          Locale locale) {
        try {
            log.info("Claim status report filter list {}", paginationRequest);
            List<ClaimStatusReportRowDTO> rows = resolveRows(paginationRequest.getSearch());
            List<ClaimStatusReportRowDTO> pagedRows = paginate(rows, paginationRequest);
            PagingResult<ClaimStatusReportRowDTO> result = new PagingResult<>(pagedRows, pagedRows.size(), rows.size());

            auditLogService.log(PAGE_CLAIM_STATUS_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(result), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.CLAIM_STATUS_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter claim status report", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<ClaimStatusReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Claim status report export {}", paginationRequest);
            List<ClaimStatusReportRowDTO> rows = resolveRows(paginationRequest.getSearch());
            byte[] excelBytes = buildExcel(rows);

            auditLogService.log(PAGE_CLAIM_STATUS_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(paginationRequest.getSearch()), null,
                    paginationRequest.getUsername());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"claim-status-report.xlsx\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export claim status report", e);
            throw e;
        }
    }

    private List<ClaimStatusReportRowDTO> resolveRows(ClaimStatusReportSearchDTO search) {
        DateRange dateRange = resolveDateRange(search);
        Map<String, String> staffCategoryDescriptions = staffCategoryResolver.loadDescriptionMap();
        return insuranceClaimsRequestRepository.findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay()).stream()
                .filter(claim -> matchesFilters(claim, search))
                .sorted(Comparator.comparing(InsuranceClaimsRequest::getCreatedDate, Comparator.nullsLast(Date::compareTo)))
                .map(claim -> mapRow(claim, staffCategoryDescriptions))
                .toList();
    }

    private boolean matchesFilters(InsuranceClaimsRequest claim, ClaimStatusReportSearchDTO search) {
        if (search == null) {
            return true;
        }
        UserPersonalDetails personalDetails = Optional.ofNullable(claim.getEmployee())
                .map(employee -> employee.getUserPersonalDetails())
                .orElse(null);
        UserCompanyDetails companyDetails = personalDetails != null ? personalDetails.getUserCompanyDetails() : null;
        ClaimsDependents dependent = claim.getClaimsDependents();

        if (hasText(search.getCompany()) && !equalsIgnoreCase(search.getCompany(),
                Optional.ofNullable(companyDetails).map(UserCompanyDetails::getCompanyTypes).map(CompanyTypes::getCode).orElse(null))) {
            return false;
        }
        if (hasText(search.getStaffCategory()) && !equalsIgnoreCase(staffCategoryResolver.normalizeSelectionCode(search.getStaffCategory()),
                staffCategoryResolver.resolveForClaim(claim))) {
            return false;
        }
        if (hasText(search.getEpfNo()) && !containsIgnoreCase(Optional.ofNullable(personalDetails).map(UserPersonalDetails::getEpfNo).orElse(null),
                search.getEpfNo())) {
            return false;
        }
        if (hasText(search.getEmployeeName()) && !containsIgnoreCase(buildEmployeeName(personalDetails), search.getEmployeeName())) {
            return false;
        }
        if (hasText(search.getDependentName()) && !containsIgnoreCase(buildDependentName(dependent), search.getDependentName())) {
            return false;
        }
        if (hasText(search.getDependentCategory()) && !matchesDependentCategory(dependent, search.getDependentCategory())) {
            return false;
        }
        if (hasText(search.getTreatment()) && !equalsIgnoreCase(search.getTreatment(), Optional.ofNullable(claim.getInsuranceClaimsDetails())
                .map(details -> details.getTreatment())
                .map(Treatment::getTreatmentCode)
                .orElse(null))) {
            return false;
        }
        if (hasText(search.getTreatmentCategory()) && !matchesTreatmentCategory(claim, search.getTreatmentCategory())) {
            return false;
        }
        return !hasText(search.getClaimStatus()) || equalsIgnoreCase(search.getClaimStatus(),
                Optional.ofNullable(claim.getRequestStatus()).map(Workflow::name).orElse(null));
    }

    private boolean matchesTreatmentCategory(InsuranceClaimsRequest claim, String filter) {
        TreatmentCategory category = Optional.ofNullable(claim.getInsuranceClaimsDetails())
                .map(details -> details.getTreatmentCategory())
                .orElse(null);
        if (category == null || !hasText(filter)) {
            return false;
        }

        String normalizedFilter = normalizeFilterValue(filter);
        String code = normalizeFilterValue(category.getCode());
        String description = normalizeFilterValue(category.getDescription());
        String codeDescription = normalizeFilterValue(buildDisplay(category.getCode(), category.getDescription()));

        return normalizedFilter.equals(code)
                || normalizedFilter.equals(description)
                || normalizedFilter.equals(codeDescription);
    }

    private ClaimStatusReportRowDTO mapRow(InsuranceClaimsRequest claim, Map<String, String> staffCategoryDescriptions) {
        ClaimStatusReportRowDTO dto = new ClaimStatusReportRowDTO();
        UserPersonalDetails personalDetails = Optional.ofNullable(claim.getEmployee())
                .map(employee -> employee.getUserPersonalDetails())
                .orElse(null);
        UserCompanyDetails companyDetails = personalDetails != null ? personalDetails.getUserCompanyDetails() : null;
        ClaimsDependents dependent = claim.getClaimsDependents();

        dto.setDate(claim.getCreatedDate());
        dto.setCompany(Optional.ofNullable(companyDetails).map(UserCompanyDetails::getCompanyTypes)
                .map(CompanyTypes::getDescription).orElse(null));
        dto.setStaffCategory(resolveStaffCategoryDescription(claim, staffCategoryDescriptions));
        dto.setEpfNumber(Optional.ofNullable(personalDetails).map(UserPersonalDetails::getEpfNo).orElse(null));
        dto.setEmployeeName(buildEmployeeName(personalDetails));
        dto.setDependentName(dependent == null ? "" : buildDependentName(dependent));
        dto.setDependentCategory(resolveDependentCategory(dependent));
        dto.setTreatmentType(Optional.ofNullable(claim.getInsuranceClaimsDetails())
                .map(details -> details.getTreatment())
                .map(Treatment::getTreatmentDescription)
                .orElse(null));
        dto.setTreatmentCategory(Optional.ofNullable(claim.getInsuranceClaimsDetails())
                .map(details -> details.getTreatmentCategory())
                .map(TreatmentCategory::getDescription)
                .orElse(null));
        dto.setRequestAmount(claim.getRequestAmount());
        dto.setApprovedAmount(claim.getApprovedAmount());
        dto.setClaimStatus(Optional.ofNullable(claim.getRequestStatus()).map(Workflow::getDescription).orElse(null));
        dto.setFinalRemark(ApprovalRemarkUtil.resolveLevelTwoOrThreeRemark(claim));
        return dto;
    }

    private String resolveStaffCategoryDescription(InsuranceClaimsRequest claim, Map<String, String> staffCategoryDescriptions) {
        String code = staffCategoryResolver.resolveForClaim(claim);
        if (!hasText(code)) {
            return "";
        }
        return staffCategoryDescriptions.getOrDefault(
                staffCategoryResolver.normalizeCode(code),
                code
        );
    }

    private List<SimpleBaseDTO> buildDependentCategoryReference() {
        List<SimpleBaseDTO> categories = Arrays.stream(RelationCategory.values())
                .map(category -> new SimpleBaseDTO(category.name(), category.getDescription()))
                .collect(Collectors.toList());
        categories.add(0, new SimpleBaseDTO(SELF, "Self"));
        return categories;
    }

    private String resolveDependentCategory(ClaimsDependents dependent) {
        if (dependent == null) {
            return SELF;
        }
        return Optional.ofNullable(dependent.getRelationCategory())
                .map(RelationCategory::getDescription)
                .orElseGet(() -> Optional.ofNullable(dependent.getDependentCategory())
                        .map(DependentCategory::getDescription)
                        .orElse(""));
    }

    private boolean matchesDependentCategory(ClaimsDependents dependent, String filter) {
        if (dependent == null) {
            return equalsIgnoreCase(filter, SELF);
        }
        return equalsIgnoreCase(filter, Optional.ofNullable(dependent.getRelationCategory()).map(RelationCategory::name).orElse(null))
                || equalsIgnoreCase(filter, Optional.ofNullable(dependent.getDependentCategory()).map(DependentCategory::name).orElse(null));
    }

    private List<ClaimStatusReportRowDTO> paginate(List<ClaimStatusReportRowDTO> rows,
                                                   PaginationRequest<ClaimStatusReportSearchDTO> paginationRequest) {
        int page = paginationRequest.getPage() != null ? paginationRequest.getPage() : 0;
        int size = paginationRequest.getSize() != null ? paginationRequest.getSize() : 10;
        int fromIndex = Math.min(page * size, rows.size());
        int toIndex = Math.min(fromIndex + size, rows.size());
        return rows.subList(fromIndex, toIndex);
    }

    private DateRange resolveDateRange(ClaimStatusReportSearchDTO search) {
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

    private byte[] buildExcel(List<ClaimStatusReportRowDTO> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Claim Status Report");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            String[] headers = {
                    "Date",
                    "Company",
                    "Staff Category",
                    "EPF Number",
                    "Employee Name",
                    "Dependent Name",
                    "Dependent Category",
                    "Treatment Type",
                    "Treatment Category",
                    "Request Amount",
                    "Approved Amount",
                    "Claim Status",
                    "Final Remark"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (ClaimStatusReportRowDTO rowDTO : rows) {
                Row row = sheet.createRow(rowIndex++);
                writeDateCell(row, 0, rowDTO.getDate(), dateStyle);
                writeTextCell(row, 1, rowDTO.getCompany(), bodyStyle);
                writeTextCell(row, 2, rowDTO.getStaffCategory(), bodyStyle);
                writeTextCell(row, 3, rowDTO.getEpfNumber(), bodyStyle);
                writeTextCell(row, 4, rowDTO.getEmployeeName(), bodyStyle);
                writeTextCell(row, 5, rowDTO.getDependentName(), bodyStyle);
                writeTextCell(row, 6, rowDTO.getDependentCategory(), bodyStyle);
                writeTextCell(row, 7, rowDTO.getTreatmentType(), bodyStyle);
                writeTextCell(row, 8, rowDTO.getTreatmentCategory(), bodyStyle);
                writeAmountCell(row, 9, rowDTO.getRequestAmount(), amountStyle);
                writeAmountCell(row, 10, rowDTO.getApprovedAmount(), amountStyle);
                writeTextCell(row, 11, rowDTO.getClaimStatus(), bodyStyle);
                writeTextCell(row, 12, rowDTO.getFinalRemark(), bodyStyle);
            }

            int[] widths = {16, 35, 28, 16, 30, 30, 22, 22, 24, 18, 18, 18, 50};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build claim status report Excel", e);
        }
    }

    private void writeTextCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void writeDateCell(Row row, int index, Date value, CellStyle style) {
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

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = createBorderedStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = createBorderedStyle(workbook);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private CellStyle createAmountStyle(Workbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
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

    private String buildEmployeeName(UserPersonalDetails details) {
        if (details == null) {
            return "";
        }
        return (Objects.toString(details.getFirstName(), "") + " " + Objects.toString(details.getLastName(), "")).trim();
    }

    private String buildDependentName(ClaimsDependents dependent) {
        if (dependent == null) {
            return "";
        }
        return (Objects.toString(dependent.getFirstName(), "") + " " + Objects.toString(dependent.getLastName(), "")).trim();
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && search != null && value.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String expected, String actual) {
        return expected != null && actual != null && expected.trim().equalsIgnoreCase(actual.trim());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
                : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizeDate(String value) {
        return value.contains("-") ? value.replace("-", "/") : value;
    }

    private record DateRange(Date startOfDay, Date endOfDay) {
    }
}

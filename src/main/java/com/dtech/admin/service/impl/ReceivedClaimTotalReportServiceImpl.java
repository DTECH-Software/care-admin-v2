package com.dtech.admin.service.impl;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.ReceivedClaimTotalReportNormalStaffDTO;
import com.dtech.admin.dto.response.ReceivedClaimTotalReportResponseDTO;
import com.dtech.admin.dto.response.ReceivedClaimTotalReportRowDTO;
import com.dtech.admin.dto.search.ReceivedClaimTotalReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.DeathClaimRequest;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.ThirdPartyIndoorClaimImportRow;
import com.dtech.admin.repository.DeathClaimRequestRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.repository.ThirdPartyIndoorClaimImportRowRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.ReceivedClaimTotalReportService;
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
import java.time.Month;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReceivedClaimTotalReportServiceImpl implements ReceivedClaimTotalReportService {

    private static final String PAGE_RECEIVED_CLAIM_TOTAL_REPORT = WebPage.RPRT_RCTR.name();
    private static final String EXEC_MIDDLE_LABEL = "EXECUTIVE & MIDDLE MANAGEMENT LEVEL STAFF";
    private static final String SENIOR_STAFF_LABEL = "SENIOR STAFF";
    private static final String NORMAL_STAFF_LABEL = "NORMAL STAFF";
    private static final String ALL_STAFF_LABEL = "ALL STAFF";
    private static final String PERIOD_SEPARATOR = " / ";
    private static final Set<String> EXEC_MIDDLE_CODES = Set.of("EXOP", "EX-OP1", "EX-OP2", "MM");
    private static final Set<String> SENIOR_STAFF_CODES = Set.of("SNR");
    private static final Set<String> NORMAL_STAFF_CODES = Set.of("NS");

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
    private final ThirdPartyIndoorClaimImportRowRepository thirdPartyIndoorClaimImportRowRepository;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final MedicalClaimStaffCategoryResolver staffCategoryResolver;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Received claim total report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_RECEIVED_CLAIM_TOTAL_REPORT);

            responseMap.put("privileges", privileges);
            responseMap.put("reportGroups", List.of(
                    new SimpleBaseDTO("NORMAL_STAFF", "Normal Staff Claims Received & Settlement Details"),
                    new SimpleBaseDTO("WECARE", "Wecare System Received Medical Claims Details"),
                    new SimpleBaseDTO("DDF", "DDF Claims Received & Settlement Details")
            ));

            auditLogService.log(PAGE_RECEIVED_CLAIM_TOTAL_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.RECEIVED_CLAIM_TOTAL_REPORT_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load received claim total report reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ReceivedClaimTotalReportSearchDTO> paginationRequest,
                                                          Locale locale) {
        try {
            log.info("Received claim total report filter list {}", paginationRequest);
            ReceivedClaimTotalReportResponseDTO responseDTO = buildReport(paginationRequest.getSearch());

            auditLogService.log(PAGE_RECEIVED_CLAIM_TOTAL_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(responseDTO), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.RECEIVED_CLAIM_TOTAL_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter received claim total report", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<ReceivedClaimTotalReportSearchDTO> paginationRequest,
                                         Locale locale) {
        try {
            log.info("Received claim total report export {}", paginationRequest);
            ReceivedClaimTotalReportResponseDTO responseDTO = buildReport(paginationRequest.getSearch());
            byte[] excelBytes = buildExcel(responseDTO);

            auditLogService.log(PAGE_RECEIVED_CLAIM_TOTAL_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(paginationRequest.getSearch()), null,
                    paginationRequest.getUsername());

            String fileName = "received-claim-total-report-" + responseDTO.getMonthTitle()
                    .toLowerCase(Locale.ROOT)
                    .replace(" ", "-") + ".xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export received claim total report", e);
            throw e;
        }
    }

    private ReceivedClaimTotalReportResponseDTO buildReport(ReceivedClaimTotalReportSearchDTO search) {
        DateRange dateRange = resolveDateRange(search);
        ReceivedClaimTotalReportResponseDTO dto = new ReceivedClaimTotalReportResponseDTO();
        dto.setPeriod(dateRange.periodText());
        dto.setMonthTitle(dateRange.monthTitle());
        dto.setNormalStaffClaims(buildNormalStaffRows(dateRange, search));
        dto.setWecareClaims(buildWecareRows(dateRange));
        dto.setDdfClaims(buildDdfRows(dateRange));
        return dto;
    }

    private ReceivedClaimTotalReportNormalStaffDTO buildNormalStaffRows(DateRange dateRange,
                                                                        ReceivedClaimTotalReportSearchDTO search) {
        List<InsuranceClaimsRequest> claims = insuranceClaimsRequestRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay()).stream()
                .filter(claim -> claim.getId() == null
                        || !thirdPartyIndoorClaimImportRowRepository.existsByInsuranceClaim_Id(claim.getId()))
                .filter(claim -> NORMAL_STAFF_CODES.contains(
                        staffCategoryResolver.normalizeCode(staffCategoryResolver.resolveForClaim(claim))))
                .toList();

        long received = claims.size();
        long settled = claims.stream()
                .filter(claim -> isSettledStatus(claim.getRequestStatus()))
                .count();
        long rejected = claims.stream()
                .filter(this::isRejectedOrPartiallyRejected)
                .count();
        long stillProcessing = claims.stream()
                .filter(claim -> Workflow.UNDER_REVIEW.equals(claim.getRequestStatus()))
                .filter(claim -> Set.of(ApprovalLevel.LEVEL02, ApprovalLevel.LEVEL03).contains(claim.getApprovalLevel()))
                .count();
        long notYetProcessed = claims.stream()
                .filter(claim -> Workflow.UNDER_REVIEW.equals(claim.getRequestStatus()))
                .filter(claim -> ApprovalLevel.LEVEL01.equals(claim.getApprovalLevel()))
                .count();
        long wecareSettled = claims.stream()
                .filter(claim -> isSettledStatus(claim.getRequestStatus()))
                .filter(this::hasLevelTwoOrThreeFinalDecision)
                .count();
        long assumeRejectClaims = search != null && search.getNormalStaffAssumeRejectClaims() != null
                ? Math.max(0, search.getNormalStaffAssumeRejectClaims())
                : 0;

        return new ReceivedClaimTotalReportNormalStaffDTO(
                NORMAL_STAFF_LABEL,
                dateRange.periodText(),
                received,
                stillProcessing,
                settled,
                rejected,
                assumeRejectClaims,
                notYetProcessed,
                wecareSettled
        );
    }

    private List<ReceivedClaimTotalReportRowDTO> buildThirdPartyRows(DateRange dateRange) {
        Map<String, ClaimCount> counts = initialMedicalGroupCounts();
        List<ThirdPartyIndoorClaimImportRow> rows = thirdPartyIndoorClaimImportRowRepository
                .findAllByIntimatedDateBetweenAndInsuranceClaimIsNotNull(dateRange.startOfDay(), dateRange.endOfDay());

        for (ThirdPartyIndoorClaimImportRow row : rows) {
            InsuranceClaimsRequest claim = row.getInsuranceClaim();
            String group = resolveMedicalGroup(claim);
            if (group == null) {
                continue;
            }
            ClaimCount count = counts.get(group);
            count.received++;
            if (claim != null && isSettledStatus(claim.getRequestStatus())) {
                count.settled++;
            }
        }
        return toMedicalRows(counts, dateRange.periodText());
    }

    private List<ReceivedClaimTotalReportRowDTO> buildWecareRows(DateRange dateRange) {
        Map<String, ClaimCount> counts = initialMedicalGroupCounts();
        List<InsuranceClaimsRequest> claims = insuranceClaimsRequestRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay());

        for (InsuranceClaimsRequest claim : claims) {
            if (claim.getId() != null && thirdPartyIndoorClaimImportRowRepository.existsByInsuranceClaim_Id(claim.getId())) {
                continue;
            }
            String group = resolveMedicalGroup(claim);
            if (group == null) {
                continue;
            }
            ClaimCount count = counts.get(group);
            count.received++;
            if (isSettledStatus(claim.getRequestStatus())) {
                count.settled++;
            }
        }
        return toMedicalRows(counts, dateRange.periodText());
    }

    private List<ReceivedClaimTotalReportRowDTO> buildDdfRows(DateRange dateRange) {
        List<DeathClaimRequest> claims = deathClaimRequestRepository
                .findAllByCreatedDateBetween(dateRange.startOfDay(), dateRange.endOfDay());

        long received = claims.size();
        long settled = claims.stream()
                .filter(claim -> isSettledStatus(claim.getRequestStatus()))
                .count();

        return List.of(new ReceivedClaimTotalReportRowDTO(
                ALL_STAFF_LABEL,
                dateRange.periodText(),
                received,
                settled,
                ""
        ));
    }

    private Map<String, ClaimCount> initialMedicalGroupCounts() {
        Map<String, ClaimCount> counts = new LinkedHashMap<>();
        counts.put(EXEC_MIDDLE_LABEL, new ClaimCount());
        counts.put(SENIOR_STAFF_LABEL, new ClaimCount());
        return counts;
    }

    private List<ReceivedClaimTotalReportRowDTO> toMedicalRows(Map<String, ClaimCount> counts, String periodText) {
        List<ReceivedClaimTotalReportRowDTO> rows = new ArrayList<>();
        counts.forEach((staffCategory, count) -> rows.add(new ReceivedClaimTotalReportRowDTO(
                staffCategory,
                periodText,
                count.received,
                count.settled,
                ""
        )));
        return rows;
    }

    private boolean isSettledStatus(Workflow status) {
        return Workflow.APPROVED.equals(status) || Workflow.REJECTED.equals(status);
    }

    private boolean hasLevelTwoOrThreeFinalDecision(InsuranceClaimsRequest claim) {
        if (claim == null) {
            return false;
        }
        return Objects.requireNonNullElse(claim.getApprovalWorkFlows(), List.<ApprovalWorkFlow>of()).stream()
                .filter(workflow -> workflow != null)
                .filter(workflow -> Set.of(ApprovalLevel.LEVEL02, ApprovalLevel.LEVEL03).contains(workflow.getApprovalLevel()))
                .anyMatch(workflow -> isSettledStatus(workflow.getStatus()));
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

    private String resolveMedicalGroup(InsuranceClaimsRequest claim) {
        if (claim == null) {
            return null;
        }
        String code = staffCategoryResolver.normalizeCode(staffCategoryResolver.resolveForClaim(claim));
        if (EXEC_MIDDLE_CODES.contains(code)) {
            return EXEC_MIDDLE_LABEL;
        }
        if (SENIOR_STAFF_CODES.contains(code)) {
            return SENIOR_STAFF_LABEL;
        }
        return null;
    }

    private DateRange resolveDateRange(ReceivedClaimTotalReportSearchDTO search) {
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

    private byte[] buildExcel(ReceivedClaimTotalReportResponseDTO report) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Received Claim Total");
            sheet.setDefaultRowHeightInPoints(20);

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook, IndexedColors.PALE_BLUE.getIndex());
            CellStyle remarkHeaderStyle = createHeaderStyle(workbook, IndexedColors.YELLOW.getIndex());
            CellStyle bodyStyle = createBodyStyle(workbook, false);
            CellStyle remarkBodyStyle = createBodyStyle(workbook, true);

            int rowIndex = 2;
            rowIndex = writeNormalStaffSection(sheet, rowIndex,
                    "MEDICAL CLAIMS RECEIVED & SETTLEMENT DETAILS - " + report.getMonthTitle(),
                    report.getNormalStaffClaims(), titleStyle, headerStyle, bodyStyle);
            rowIndex += 2;
            rowIndex = writeSection(sheet, rowIndex,
                    "WECARE SYSTEM RECEIVED MEDICAL CLAIMS DETAILS - " + report.getMonthTitle(),
                    report.getWecareClaims(), titleStyle, headerStyle, remarkHeaderStyle, bodyStyle, remarkBodyStyle);
            rowIndex += 2;
            writeSection(sheet, rowIndex,
                    "DDF CLAIMS RECEIVED & SETTLEMENT DETAILS - " + report.getMonthTitle(),
                    report.getDdfClaims(), titleStyle, headerStyle, remarkHeaderStyle, bodyStyle, remarkBodyStyle);

            int[] widths = {45, 28, 24, 28, 28, 24, 22, 24};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build received claim total report Excel", e);
        }
    }

    private int writeNormalStaffSection(Sheet sheet,
                                        int startRow,
                                        String title,
                                        ReceivedClaimTotalReportNormalStaffDTO rowDTO,
                                        CellStyle titleStyle,
                                        CellStyle headerStyle,
                                        CellStyle bodyStyle) {
        Row titleRow = sheet.createRow(startRow);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 7));

        Row headerRow = sheet.createRow(startRow + 2);
        headerRow.setHeightInPoints(52);
        String[] headers = {
                "STAFF CATEGORY",
                "CLAIM RECEIVED PERIOD",
                "NO OF RECEIVED CLAIMS",
                "STILL PROCESSING CLAIMS",
                "NO OF SETTLED CLAIMS",
                "NO OF REJECTED CLAIMS",
                "NOT YET PROCESSED",
                "NO OF SETTLED CLAIMS - WECARE"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = startRow + 3;
        if (rowDTO != null) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(28);
            writeCell(row, 0, rowDTO.getStaffCategory(), bodyStyle);
            writeCell(row, 1, rowDTO.getClaimReceivedPeriod(), bodyStyle);
            writeCell(row, 2, rowDTO.getReceivedClaims(), bodyStyle);
            writeCell(row, 3, rowDTO.getStillProcessingClaims(), bodyStyle);
            writeCell(row, 4, rowDTO.getSettledClaims(), bodyStyle);
            writeCell(row, 5, rowDTO.getRejectedClaims(), bodyStyle);
            writeCell(row, 6, rowDTO.getNotYetProcessedClaims(), bodyStyle);
            writeCell(row, 7, rowDTO.getWecareSettledClaims(), bodyStyle);
        }
        return rowIndex;
    }

    private int writeSection(Sheet sheet,
                             int startRow,
                             String title,
                             List<ReceivedClaimTotalReportRowDTO> rows,
                             CellStyle titleStyle,
                             CellStyle headerStyle,
                             CellStyle remarkHeaderStyle,
                             CellStyle bodyStyle,
                             CellStyle remarkBodyStyle) {
        Row titleRow = sheet.createRow(startRow);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 4));

        Row headerRow = sheet.createRow(startRow + 2);
        headerRow.setHeightInPoints(35);
        String[] headers = {
                "STAFF CATEGORY",
                "CLAIM RECEIVED PERIOD",
                "NO. OF RECEIVED CLAIMS",
                "NO. OF SETTLED CLAIMS",
                "REMARK"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(i == 4 ? remarkHeaderStyle : headerStyle);
        }

        int rowIndex = startRow + 3;
        for (ReceivedClaimTotalReportRowDTO rowDTO : Objects.requireNonNullElse(rows, List.<ReceivedClaimTotalReportRowDTO>of())) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(28);
            writeCell(row, 0, rowDTO.getStaffCategory(), bodyStyle);
            writeCell(row, 1, rowDTO.getClaimReceivedPeriod(), bodyStyle);
            writeCell(row, 2, rowDTO.getReceivedClaims(), bodyStyle);
            writeCell(row, 3, rowDTO.getSettledClaims(), bodyStyle);
            writeCell(row, 4, rowDTO.getRemark(), remarkBodyStyle);
        }
        return rowIndex;
    }

    private void writeCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void writeCell(Row row, int index, long value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
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

    private CellStyle createHeaderStyle(Workbook workbook, short fillColor) {
        CellStyle style = createBorderedStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(fillColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook, boolean remark) {
        CellStyle style = createBorderedStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        if (remark) {
            style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
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

    private String normalizeDate(String value) {
        return value.contains("-") ? value.replace("-", "/") : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static class ClaimCount {
        private long received;
        private long settled;
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
}

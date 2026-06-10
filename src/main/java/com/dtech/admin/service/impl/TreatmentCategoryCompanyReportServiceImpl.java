package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.TreatmentCategoryCompanyReportRowDTO;
import com.dtech.admin.dto.search.TreatmentCategoryCompanyReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.InsuranceClaimsDetails;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.Treatment;
import com.dtech.admin.model.TreatmentCategory;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.repository.TreatmentCategoryRepository;
import com.dtech.admin.repository.TreatmentRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.TreatmentCategoryCompanyReportService;
import com.dtech.admin.specifications.TreatmentCategoryCompanyReportSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
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

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class TreatmentCategoryCompanyReportServiceImpl implements TreatmentCategoryCompanyReportService {

    private static final String PAGE_TCCR_REPORT = WebPage.RPRT_TCCR.name();

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
    private final TreatmentRepository treatmentRepository;

    @Autowired
    private final TreatmentCategoryRepository treatmentCategoryRepository;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Treatment category company report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new LinkedHashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_TCCR_REPORT);

            List<SimpleBaseDTO> company = companyTypeRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> treatment = treatmentRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getTreatmentCode(), val.getTreatmentDescription())).toList();

            List<SimpleBaseDTO> treatmentCategory = treatmentCategoryRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();
            List<SimpleBaseDTO> staffCategories = staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("company", company);
            responseMap.put("treatment", treatment);
            responseMap.put("treatmentCategory", treatmentCategory);
            responseMap.put("staffCategories", staffCategories);

            auditLogService.log(PAGE_TCCR_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.TCCR_REPORT_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load treatment category company report reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<TreatmentCategoryCompanyReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Treatment category company report filter list {}", paginationRequest);
            List<TreatmentCategoryCompanyReportRowDTO> rows = resolveRows(paginationRequest.getSearch());
            List<TreatmentCategoryCompanyReportRowDTO> sortedRows = sortRows(rows, paginationRequest);
            PagingResult<TreatmentCategoryCompanyReportRowDTO> result = buildPagingResult(sortedRows, paginationRequest);

            auditLogService.log(PAGE_TCCR_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(result.getContent()), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.TCCR_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter treatment category company report", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<TreatmentCategoryCompanyReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Treatment category company report export {}", paginationRequest);
            List<TreatmentCategoryCompanyReportRowDTO> rows = resolveRows(paginationRequest.getSearch());
            rows = sortRows(rows, paginationRequest);

            byte[] excelBytes = buildExcel(rows);
            auditLogService.log(PAGE_TCCR_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(paginationRequest.getSearch()), null, paginationRequest.getUsername());

            String fileName = "treatment-category-company-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export treatment category company report", e);
            throw e;
        }
    }

    private List<TreatmentCategoryCompanyReportRowDTO> resolveRows(TreatmentCategoryCompanyReportSearchDTO search) {
        List<InsuranceClaimsRequest> claims = insuranceClaimsRequestRepository
                .findAll(TreatmentCategoryCompanyReportSpecification.getSpecification(search));

        Map<String, TreatmentCategoryCompanyReportRowDTO> summary = new LinkedHashMap<>();
        for (InsuranceClaimsRequest claim : claims) {
            InsuranceClaimsDetails details = claim.getInsuranceClaimsDetails();
            Treatment treatment = details != null ? details.getTreatment() : null;
            TreatmentCategory category = details != null ? details.getTreatmentCategory() : null;

            UserPersonalDetails personalDetails = claim.getEmployee() != null
                    ? claim.getEmployee().getUserPersonalDetails()
                    : null;
            UserCompanyDetails companyDetails = personalDetails != null ? personalDetails.getUserCompanyDetails() : null;
            CompanyTypes companyType = companyDetails != null ? companyDetails.getCompanyTypes() : null;
            StaffCategories staffCategories = resolveClaimStaffCategory(claim);

            String companyCode = companyType != null ? companyType.getCode() : "";
            String staffCategoryCode = staffCategories != null ? staffCategories.getCode() : "";
            String treatmentCode = treatment != null ? treatment.getTreatmentCode() : "";
            String categoryCode = category != null ? category.getCode() : "";

            String key = companyCode + "|" + staffCategoryCode + "|" + treatmentCode + "|" + categoryCode;
            TreatmentCategoryCompanyReportRowDTO row = summary.computeIfAbsent(key, k -> {
                TreatmentCategoryCompanyReportRowDTO dto = new TreatmentCategoryCompanyReportRowDTO();
                dto.setCompanyCode(companyCode);
                dto.setCompanyDescription(companyType != null ? companyType.getDescription() : "");
                dto.setStaffCategoryCode(staffCategoryCode);
                dto.setStaffCategoryDescription(staffCategories != null ? staffCategories.getDescription() : "");
                dto.setTreatmentCode(treatmentCode);
                dto.setTreatmentDescription(treatment != null ? treatment.getTreatmentDescription() : "");
                dto.setTreatmentCategoryCode(categoryCode);
                dto.setTreatmentCategoryDescription(category != null ? category.getDescription() : "");
                dto.setRequestTotalAmount(BigDecimal.ZERO);
                dto.setApprovedTotalAmount(BigDecimal.ZERO);
                dto.setRemainingBalance(BigDecimal.ZERO);
                dto.setL1Remark("");
                dto.setL2Remark("");
                dto.setL3Remark("");
                return dto;
            });

            BigDecimal requestAmount = claim.getRequestAmount() != null ? claim.getRequestAmount() : BigDecimal.ZERO;
            BigDecimal approvedAmount = claim.getApprovedAmount() != null ? claim.getApprovedAmount() : BigDecimal.ZERO;
            row.setRequestTotalAmount(row.getRequestTotalAmount().add(requestAmount));
            row.setApprovedTotalAmount(row.getApprovedTotalAmount().add(approvedAmount));
            row.setRemainingBalance(calculateRemainingBalance(
                    row.getRequestTotalAmount(),
                    row.getApprovedTotalAmount()
            ));
            appendWorkflowRemarks(row, claim);
        }

        return new ArrayList<>(summary.values());
    }

    private StaffCategories resolveClaimStaffCategory(InsuranceClaimsRequest claim) {
        if (claim == null) {
            return null;
        }
        InsuranceStaffCategoryPeriod period = null;
        if (claim.getInsuranceDetailsLimit() != null) {
            period = claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod();
        }
        if (period == null && claim.getInsuranceClaimsDetails() != null) {
            period = claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod();
        }
        return period != null ? period.getStaffCategories() : null;
    }

    private List<TreatmentCategoryCompanyReportRowDTO> sortRows(List<TreatmentCategoryCompanyReportRowDTO> rows,
                                                               PaginationRequest<TreatmentCategoryCompanyReportSearchDTO> paginationRequest) {
        String sortColumn = resolveSortColumn(paginationRequest.getSortColumn());
        if (sortColumn == null) {
            return rows;
        }

        Comparator<TreatmentCategoryCompanyReportRowDTO> comparator = switch (sortColumn) {
            case "companyCode" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getCompanyCode,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "treatmentCode" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getTreatmentCode,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "treatmentCategoryCode" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getTreatmentCategoryCode,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "staffCategoryCode" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getStaffCategoryCode,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "requestTotalAmount" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getRequestTotalAmount,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "approvedTotalAmount" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getApprovedTotalAmount,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "remainingBalance" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getRemainingBalance,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "l1Remark" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getL1Remark,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "l2Remark" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getL2Remark,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "l3Remark" -> Comparator.comparing(TreatmentCategoryCompanyReportRowDTO::getL3Remark,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            default -> null;
        };

        if (comparator == null) {
            return rows;
        }

        if (paginationRequest.getSortDirection() == Sort.Direction.DESC) {
            comparator = comparator.reversed();
        }

        List<TreatmentCategoryCompanyReportRowDTO> sortedRows = new ArrayList<>(rows);
        sortedRows.sort(comparator);
        return sortedRows;
    }

    private String resolveSortColumn(String sortColumn) {
        if (sortColumn == null || sortColumn.isBlank()) {
            return null;
        }
        return switch (sortColumn) {
            case "company", "companyCode" -> "companyCode";
            case "staffCategory", "staffCategoryCode" -> "staffCategoryCode";
            case "treatment", "treatmentCode" -> "treatmentCode";
            case "treatmentCategory", "treatmentCategoryCode" -> "treatmentCategoryCode";
            case "requestTotalAmount" -> "requestTotalAmount";
            case "approvedTotalAmount" -> "approvedTotalAmount";
            case "remainingBalance" -> "remainingBalance";
            case "l1Remark" -> "l1Remark";
            case "l2Remark" -> "l2Remark";
            case "l3Remark" -> "l3Remark";
            default -> null;
        };
    }

    private PagingResult<TreatmentCategoryCompanyReportRowDTO> buildPagingResult(
            List<TreatmentCategoryCompanyReportRowDTO> rows,
            PaginationRequest<TreatmentCategoryCompanyReportSearchDTO> paginationRequest) {
        int page = paginationRequest.getPage() != null ? paginationRequest.getPage() : 0;
        int size = paginationRequest.getSize() != null ? paginationRequest.getSize() : rows.size();
        int fromIndex = Math.min(page * size, rows.size());
        int toIndex = Math.min(fromIndex + size, rows.size());
        List<TreatmentCategoryCompanyReportRowDTO> content = rows.subList(fromIndex, toIndex);
        return new PagingResult<>(content, content.size(), rows.size());
    }

    private byte[] buildExcel(List<TreatmentCategoryCompanyReportRowDTO> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Treatment Category Report");

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
            sheet.setColumnWidth(1, 24 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 24 * 256);
            sheet.setColumnWidth(4, 24 * 256);
            sheet.setColumnWidth(5, 18 * 256);
            sheet.setColumnWidth(6, 18 * 256);
            sheet.setColumnWidth(7, 18 * 256);
            sheet.setColumnWidth(8, 35 * 256);
            sheet.setColumnWidth(9, 35 * 256);
            sheet.setColumnWidth(10, 35 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("Treatment Category Company Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 10));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Company", headerStyle);
            createStringCell(row, 2, "Staff Category", headerStyle);
            createStringCell(row, 3, "Treatment", headerStyle);
            createStringCell(row, 4, "Treatment Category", headerStyle);
            createStringCell(row, 5, "Request Total Amount", headerStyle);
            createStringCell(row, 6, "Approved Total Amount", headerStyle);
            createStringCell(row, 7, "Remaining Balance", headerStyle);
            createStringCell(row, 8, "L1 Remark", headerStyle);
            createStringCell(row, 9, "L2 Remark", headerStyle);
            createStringCell(row, 10, "L3 Remark", headerStyle);

            int lineNo = 1;
            for (TreatmentCategoryCompanyReportRowDTO rowDTO : rows) {
                row = sheet.createRow(rowIndex++);
                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, buildDisplay(rowDTO.getCompanyCode(), rowDTO.getCompanyDescription()), dataStyle);
                createStringCell(row, 2, descriptionOnly(rowDTO.getStaffCategoryCode(), rowDTO.getStaffCategoryDescription()), dataStyle);
                createStringCell(row, 3, descriptionOnly(rowDTO.getTreatmentCode(), rowDTO.getTreatmentDescription()), dataStyle);
                createStringCell(row, 4, descriptionOnly(rowDTO.getTreatmentCategoryCode(), rowDTO.getTreatmentCategoryDescription()), dataStyle);
                createStringCell(row, 5, toAmountString(rowDTO.getRequestTotalAmount()), dataStyle);
                createStringCell(row, 6, toAmountString(rowDTO.getApprovedTotalAmount()), dataStyle);
                createStringCell(row, 7, toAmountString(rowDTO.getRemainingBalance()), dataStyle);
                createStringCell(row, 8, rowDTO.getL1Remark(), dataStyle);
                createStringCell(row, 9, rowDTO.getL2Remark(), dataStyle);
                createStringCell(row, 10, rowDTO.getL3Remark(), dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate treatment category company report excel", e);
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
        if (code == null || code.isBlank()) {
            return "";
        }
        if (description == null || description.isBlank()) {
            return code;
        }
        return code + " - " + description;
    }

    private String descriptionOnly(String code, String description) {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return code != null ? code : "";
    }

    private void appendWorkflowRemarks(TreatmentCategoryCompanyReportRowDTO row, InsuranceClaimsRequest claim) {
        if (claim.getApprovalWorkFlows() == null || claim.getApprovalWorkFlows().isEmpty()) {
            return;
        }

        for (ApprovalWorkFlow workflow : claim.getApprovalWorkFlows()) {
            if (workflow.getRejectedRemark() == null || workflow.getRejectedRemark().isBlank()
                    || workflow.getApprovalLevel() == null) {
                continue;
            }

            String remark = claim.getRequestId() + " - " + workflow.getRejectedRemark().trim();
            switch (workflow.getApprovalLevel()) {
                case LEVEL01 -> row.setL1Remark(appendRemark(row.getL1Remark(), remark));
                case LEVEL02 -> row.setL2Remark(appendRemark(row.getL2Remark(), remark));
                case LEVEL03 -> row.setL3Remark(appendRemark(row.getL3Remark(), remark));
            }
        }
    }

    private String appendRemark(String existing, String remark) {
        if (existing == null || existing.isBlank()) {
            return remark;
        }
        if (existing.contains(remark)) {
            return existing;
        }
        return existing + "; " + remark;
    }

    private String toAmountString(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "0";
    }

    private BigDecimal calculateRemainingBalance(BigDecimal requestAmount, BigDecimal approvedAmount) {
        BigDecimal request = requestAmount != null ? requestAmount : BigDecimal.ZERO;
        BigDecimal approved = approvedAmount != null ? approvedAmount : BigDecimal.ZERO;
        return request.subtract(approved);
    }
}

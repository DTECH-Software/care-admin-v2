package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeCountReportRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.EmployeeCountReportRowDTO;
import com.dtech.admin.dto.search.EmployeeCountReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.repository.UserPersonalDetailsRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.EmployeeCountReportService;
import com.dtech.admin.specifications.EmployeeCountReportSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmployeeCountReportServiceImpl implements EmployeeCountReportService {

    private static final String PAGE_EMPLOYEE_COUNT_REPORT = WebPage.RPEN.name();

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
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final UserPersonalDetailsRepository userPersonalDetailsRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Employee count report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_EMPLOYEE_COUNT_REPORT);

            List<SimpleBaseDTO> company = companyTypeRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> staffCategories = staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("company", company);
            responseMap.put("staffCategories", staffCategories);

            auditLogService.log(PAGE_EMPLOYEE_COUNT_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS,
                            new Object[]{PAGE_EMPLOYEE_COUNT_REPORT}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeCountReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Employee count report filter list {}", paginationRequest);
            List<EmployeeCountReportRowDTO> rows = resolveRows(paginationRequest.getSearch());
            List<EmployeeCountReportRowDTO> sortedRows = sortRows(rows, paginationRequest);
            PagingResult<EmployeeCountReportRowDTO> result = buildPagingResult(sortedRows, paginationRequest);

            auditLogService.log(PAGE_EMPLOYEE_COUNT_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(result.getContent()), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_COUNT_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(EmployeeCountReportRequestDTO employeeCountReportRequestDTO, Locale locale) {
        try {
            log.info("Employee count report view {}", employeeCountReportRequestDTO);
            EmployeeCountReportSearchDTO search = new EmployeeCountReportSearchDTO();
            search.setCompany(employeeCountReportRequestDTO.getCompany());
            search.setStaffCategory(employeeCountReportRequestDTO.getStaffCategory());
            search.setStatus(List.of(employeeCountReportRequestDTO.getStatus()));

            List<EmployeeCountReportRowDTO> rows = resolveRows(search);
            if (rows.isEmpty()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 1050,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_COUNT_REPORT_NOT_FOUND, null, locale)));
            }

            EmployeeCountReportRowDTO row = rows.get(0);
            auditLogService.log(PAGE_EMPLOYEE_COUNT_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), employeeCountReportRequestDTO.getIp(),
                    employeeCountReportRequestDTO.getUserAgent(), gson.toJson(row), null, employeeCountReportRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) row,
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_COUNT_REPORT_VIEW_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<EmployeeCountReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Employee count report export {}", paginationRequest);
            List<EmployeeCountReportRowDTO> rows = resolveRows(paginationRequest.getSearch());
            rows = sortRows(rows, paginationRequest);

            byte[] excelBytes = buildExcel(rows);
            auditLogService.log(PAGE_EMPLOYEE_COUNT_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(paginationRequest), null, paginationRequest.getUsername());

            String fileName = "employee-count-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export employee count report", e);
            throw e;
        }
    }

    private List<EmployeeCountReportRowDTO> resolveRows(EmployeeCountReportSearchDTO search) {
        List<UserPersonalDetails> employees = Objects.nonNull(search)
                ? userPersonalDetailsRepository.findAll(EmployeeCountReportSpecification.getSpecification(search))
                : userPersonalDetailsRepository.findAll(EmployeeCountReportSpecification.getSpecification());
        return buildCounts(employees);
    }

    private List<EmployeeCountReportRowDTO> sortRows(List<EmployeeCountReportRowDTO> rows,
                                                     PaginationRequest<EmployeeCountReportSearchDTO> paginationRequest) {
        String sortColumn = resolveSortColumn(paginationRequest.getSortColumn());
        if (sortColumn == null) {
            return rows;
        }

        Comparator<EmployeeCountReportRowDTO> comparator = switch (sortColumn) {
            case "companyCode" -> Comparator.comparing(EmployeeCountReportRowDTO::getCompanyCode,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "staffCategoryCode" -> Comparator.comparing(EmployeeCountReportRowDTO::getStaffCategoryCode,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "status" -> Comparator.comparing(EmployeeCountReportRowDTO::getStatus,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "employeeCount" -> Comparator.comparingLong(EmployeeCountReportRowDTO::getEmployeeCount);
            default -> null;
        };

        if (comparator == null) {
            return rows;
        }

        if (paginationRequest.getSortDirection() == Sort.Direction.DESC) {
            comparator = comparator.reversed();
        }

        List<EmployeeCountReportRowDTO> sortedRows = new ArrayList<>(rows);
        sortedRows.sort(comparator);
        return sortedRows;
    }

    private PagingResult<EmployeeCountReportRowDTO> buildPagingResult(List<EmployeeCountReportRowDTO> rows,
                                                                      PaginationRequest<EmployeeCountReportSearchDTO> paginationRequest) {
        int page = paginationRequest.getPage() != null ? paginationRequest.getPage() : 0;
        int size = paginationRequest.getSize() != null ? paginationRequest.getSize() : rows.size();
        int fromIndex = Math.min(page * size, rows.size());
        int toIndex = Math.min(fromIndex + size, rows.size());
        List<EmployeeCountReportRowDTO> content = rows.subList(fromIndex, toIndex);
        return new PagingResult<>(content, content.size(), rows.size());
    }

    private String resolveSortColumn(String sortColumn) {
        if (sortColumn == null || sortColumn.isBlank()) {
            return null;
        }
        return switch (sortColumn) {
            case "company" -> "companyCode";
            case "companyCode" -> "companyCode";
            case "staffCategory" -> "staffCategoryCode";
            case "staffCategoryCode" -> "staffCategoryCode";
            case "employeeCount" -> "employeeCount";
            case "status" -> "status";
            default -> null;
        };
    }

    private List<EmployeeCountReportRowDTO> buildCounts(List<UserPersonalDetails> employees) {
        Map<String, EmployeeCountReportRowDTO> summary = new LinkedHashMap<>();
        for (UserPersonalDetails details : employees) {
            UserCompanyDetails companyDetails = details.getUserCompanyDetails();
            String companyCode = companyDetails != null && companyDetails.getCompanyTypes() != null
                    ? companyDetails.getCompanyTypes().getCode() : "";
            String companyDescription = companyDetails != null && companyDetails.getCompanyTypes() != null
                    ? companyDetails.getCompanyTypes().getDescription() : "";
            String staffCategoryCode = companyDetails != null && companyDetails.getStaffCategories() != null
                    ? companyDetails.getStaffCategories().getCode() : "";
            String staffCategoryDescription = companyDetails != null && companyDetails.getStaffCategories() != null
                    ? companyDetails.getStaffCategories().getDescription() : "";
            String statusCode = details.getUserStatus() != null ? details.getUserStatus().name() : "";
            String statusDescription = details.getUserStatus() != null ? details.getUserStatus().getDescription() : "";

            String key = companyCode + "|" + staffCategoryCode + "|" + statusCode;
            EmployeeCountReportRowDTO row = summary.computeIfAbsent(key, k -> {
                EmployeeCountReportRowDTO dto = new EmployeeCountReportRowDTO();
                dto.setCompanyCode(companyCode);
                dto.setCompanyDescription(companyDescription);
                dto.setStaffCategoryCode(staffCategoryCode);
                dto.setStaffCategoryDescription(staffCategoryDescription);
                dto.setStatus(statusCode);
                dto.setStatusDescription(statusDescription);
                dto.setEmployeeCount(0L);
                return dto;
            });
            row.setEmployeeCount(row.getEmployeeCount() + 1);
        }

        List<EmployeeCountReportRowDTO> rows = new ArrayList<>(summary.values());
        rows.sort(Comparator
                .comparing(EmployeeCountReportRowDTO::getCompanyCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(EmployeeCountReportRowDTO::getStaffCategoryCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(EmployeeCountReportRowDTO::getStatus, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private byte[] buildExcel(List<EmployeeCountReportRowDTO> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employee Count Report");

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
            sheet.setColumnWidth(1, 22 * 256);
            sheet.setColumnWidth(2, 22 * 256);
            sheet.setColumnWidth(3, 18 * 256);
            sheet.setColumnWidth(4, 10 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("Employee Count Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Company", headerStyle);
            createStringCell(row, 2, "Staff Category", headerStyle);
            createStringCell(row, 3, "Status", headerStyle);
            createStringCell(row, 4, "Employee Count", headerStyle);

            int lineNo = 1;
            long totalEmployees = 0L;
            for (EmployeeCountReportRowDTO rowDTO : rows) {
                row = sheet.createRow(rowIndex++);
                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, buildDisplay(rowDTO.getCompanyCode(), rowDTO.getCompanyDescription()), dataStyle);
                createStringCell(row, 2, buildDisplay(rowDTO.getStaffCategoryCode(), rowDTO.getStaffCategoryDescription()), dataStyle);
                createStringCell(row, 3, buildDisplay(rowDTO.getStatus(), rowDTO.getStatusDescription()), dataStyle);
                createStringCell(row, 4, String.valueOf(rowDTO.getEmployeeCount()), dataStyle);
                totalEmployees += rowDTO.getEmployeeCount();
            }

            rowIndex++;
            Row totalRow = sheet.createRow(rowIndex);
            createStringCell(totalRow, 0, "", dataStyle);
            createStringCell(totalRow, 1, "", dataStyle);
            createStringCell(totalRow, 2, "", dataStyle);
            createStringCell(totalRow, 3, "Total", dataStyle);
            createStringCell(totalRow, 4, String.valueOf(totalEmployees), dataStyle);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate employee count report excel", e);
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
}

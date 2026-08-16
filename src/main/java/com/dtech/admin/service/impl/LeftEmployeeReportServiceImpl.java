package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.EmployeeDetailsResponseDTO;
import com.dtech.admin.dto.response.LeftEmployeeReportRowDTO;
import com.dtech.admin.dto.search.LeftEmployeeReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Facility;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.mapper.entityToDto.EmployeeDetailsMapperEntityToDto;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.repository.UserPersonalDetailsRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.CompanyAccessService;
import com.dtech.admin.service.LeftEmployeeReportService;
import com.dtech.admin.specifications.CompanyScopeSpecification;
import com.dtech.admin.specifications.LeftEmployeeReportSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class LeftEmployeeReportServiceImpl implements LeftEmployeeReportService {

    private static final String PAGE_LEFT_EMPLOYEE_REPORT = WebPage.RPEL.name();

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
    private final CompanyAccessService companyAccessService;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final UserPersonalDetailsRepository userPersonalDetailsRepository;

    @Autowired
    private final EmployeeDetailsMapperEntityToDto employeeDetailsMapperEntityToDto;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Left employee report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_LEFT_EMPLOYEE_REPORT);

            List<SimpleBaseDTO> facility = Arrays.stream(Facility.values())
                    .map(val -> new SimpleBaseDTO(val.name(), val.getDescription()))
                    .toList();

            List<SimpleBaseDTO> company = companyAccessService.activeCompanies(channelRequestDTO.getUsername())
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> staffCategories = staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("facility", facility);
            responseMap.put("company", company);
            responseMap.put("staffCategories", staffCategories);

            auditLogService.log(PAGE_LEFT_EMPLOYEE_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS,
                            new Object[]{PAGE_LEFT_EMPLOYEE_REPORT}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<LeftEmployeeReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Left employee report filter list {}", paginationRequest);
            normalizeSortColumn(paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            LeftEmployeeReportSearchDTO search = paginationRequest.getSearch();
            Specification<UserPersonalDetails> specification = scopedSpecification(search, paginationRequest.getUsername());
            Page<UserPersonalDetails> employeePage = userPersonalDetailsRepository.findAll(specification, pageable);
            long totalElements = userPersonalDetailsRepository.count(specification);

            List<EmployeeDetailsResponseDTO> rows = employeePage.stream()
                    .map(employeeDetailsMapperEntityToDto::mapEmployeeDetails)
                    .toList();

            PagingResult<EmployeeDetailsResponseDTO> result = new PagingResult<>(rows, rows.size(), totalElements);

            auditLogService.log(PAGE_LEFT_EMPLOYEE_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(rows), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<LeftEmployeeReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Left employee report export {}", paginationRequest);
            LeftEmployeeReportSearchDTO search = paginationRequest.getSearch();

            List<UserPersonalDetails> employees = userPersonalDetailsRepository.findAll(
                    scopedSpecification(search, paginationRequest.getUsername()));

            List<LeftEmployeeReportRowDTO> rows = employees.stream()
                    .map(this::mapRow)
                    .toList();

            byte[] excelBytes = buildExcel(rows);
            auditLogService.log(PAGE_LEFT_EMPLOYEE_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(search), null, paginationRequest.getUsername());

            String fileName = "left-employee-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export left employee report", e);
            throw e;
        }
    }

    private LeftEmployeeReportRowDTO mapRow(UserPersonalDetails details) {
        LeftEmployeeReportRowDTO dto = new LeftEmployeeReportRowDTO();
        dto.setEmployeeId(details.getId());
        dto.setEpf(details.getEpfNo());
        dto.setEmployeeName(buildEmployeeName(details));
        dto.setStatus(details.getUserStatus().name());
        dto.setStatusDescription(details.getUserStatus().getDescription());

        UserCompanyDetails companyDetails = details.getUserCompanyDetails();
        if (companyDetails != null) {
            if (companyDetails.getCompanyTypes() != null) {
                dto.setCompanyCode(companyDetails.getCompanyTypes().getCode());
                dto.setCompanyDescription(companyDetails.getCompanyTypes().getDescription());
            }
            if (companyDetails.getStaffCategories() != null) {
                dto.setStaffCategoryCode(companyDetails.getStaffCategories().getCode());
                dto.setStaffCategoryDescription(companyDetails.getStaffCategories().getDescription());
            }
            if (companyDetails.getFacility() != null) {
                dto.setFacility(companyDetails.getFacility().name());
                dto.setFacilityDescription(companyDetails.getFacility().getDescription());
            }
            dto.setTerminateDate(companyDetails.getTerminateDate());
        }
        return dto;
    }

    private Specification<UserPersonalDetails> scopedSpecification(LeftEmployeeReportSearchDTO search, String username) {
        Specification<UserPersonalDetails> requested = search == null
                ? LeftEmployeeReportSpecification.getSpecification()
                : LeftEmployeeReportSpecification.getSpecification(search);
        return requested.and(CompanyScopeSpecification.companyCodeIn(companyAccessService.activeCompanyCodes(username),
                "userCompanyDetails", "companyTypes", "code"));
    }

    private void normalizeSortColumn(PaginationRequest<LeftEmployeeReportSearchDTO> paginationRequest) {
        String sortColumn = paginationRequest.getSortColumn();
        if (sortColumn == null || sortColumn.isBlank()) {
            return;
        }
        switch (sortColumn) {
            case "terminateDate" -> paginationRequest.setSortColumn("userCompanyDetails.terminateDate");
            case "facility" -> paginationRequest.setSortColumn("userCompanyDetails.facility");
            case "company" -> paginationRequest.setSortColumn("userCompanyDetails.companyTypes.code");
            case "staffCategory" -> paginationRequest.setSortColumn("userCompanyDetails.staffCategories.code");
            case "status" -> paginationRequest.setSortColumn("userStatus");
            case "epf" -> paginationRequest.setSortColumn("epfNo");
            default -> {
                // Keep provided sort column.
            }
        }
    }

    private String buildEmployeeName(UserPersonalDetails details) {
        String firstName = details.getFirstName() != null ? details.getFirstName().trim() : "";
        String lastName = details.getLastName() != null ? details.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? details.getInitials() : fullName;
    }

    private byte[] buildExcel(List<LeftEmployeeReportRowDTO> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Left Employee Report");

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
            sheet.setColumnWidth(1, 12 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            sheet.setColumnWidth(3, 22 * 256);
            sheet.setColumnWidth(4, 22 * 256);
            sheet.setColumnWidth(5, 18 * 256);
            sheet.setColumnWidth(6, 18 * 256);
            sheet.setColumnWidth(7, 16 * 256);
            sheet.setColumnWidth(8, 18 * 256);
            sheet.setColumnWidth(9, 16 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("Left Employee Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 9));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Employee ID", headerStyle);
            createStringCell(row, 2, "EPF", headerStyle);
            createStringCell(row, 3, "Employee Name", headerStyle);
            createStringCell(row, 4, "Company", headerStyle);
            createStringCell(row, 5, "Staff Category", headerStyle);
            createStringCell(row, 6, "Facility", headerStyle);
            createStringCell(row, 7, "Terminate Date", headerStyle);
            createStringCell(row, 8, "Status", headerStyle);
            createStringCell(row, 9, "Status Description", headerStyle);

            int lineNo = 1;
            for (LeftEmployeeReportRowDTO rowDTO : rows) {
                row = sheet.createRow(rowIndex++);
                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, rowDTO.getEmployeeId() != null ? rowDTO.getEmployeeId().toString() : "", dataStyle);
                createStringCell(row, 2, rowDTO.getEpf(), dataStyle);
                createStringCell(row, 3, rowDTO.getEmployeeName(), dataStyle);
                createStringCell(row, 4, buildDisplay(rowDTO.getCompanyCode(), rowDTO.getCompanyDescription()), dataStyle);
                createStringCell(row, 5, buildDisplay(rowDTO.getStaffCategoryCode(), rowDTO.getStaffCategoryDescription()), dataStyle);
                createStringCell(row, 6, buildDisplay(rowDTO.getFacility(), rowDTO.getFacilityDescription()), dataStyle);
                createStringCell(row, 7, formatDate(rowDTO.getTerminateDate()), dataStyle);
                createStringCell(row, 8, rowDTO.getStatus(), dataStyle);
                createStringCell(row, 9, rowDTO.getStatusDescription(), dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate left employee report excel", e);
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

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }
}

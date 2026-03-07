package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.EmployeeDetailsResponseDTO;
import com.dtech.admin.dto.search.EmployeeReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Facility;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.mapper.entityToDto.EmployeeDetailsMapperEntityToDto;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.repository.UserPersonalDetailsRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.EmployeeReportService;
import com.dtech.admin.specifications.EmployeeReportSpecification;
import com.dtech.admin.util.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
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

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmployeeReportServiceImpl implements EmployeeReportService {

    private static final String PAGE_EMPLOYEE_REPORT = WebPage.RPEM.name();

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

    @Autowired
    private final EmployeeDetailsMapperEntityToDto employeeDetailsMapperEntityToDto;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Employee report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_EMPLOYEE_REPORT);

            List<SimpleBaseDTO> status = Arrays.stream(Status.values())
                    .filter(st -> !Status.DELETE.equals(st))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> facility = Arrays.stream(Facility.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> company = companyTypeRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> staffCategories = staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("status", status);
            responseMap.put("facility", facility);
            responseMap.put("company", company);
            responseMap.put("staffCategories", staffCategories);

            auditLogService.log(PAGE_EMPLOYEE_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS,
                            new Object[]{PAGE_EMPLOYEE_REPORT}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Employee report filter list {}", paginationRequest);
            normalizeSortColumn(paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            EmployeeReportSearchDTO search = paginationRequest.getSearch();
            Page<UserPersonalDetails> employeePage = Objects.nonNull(search)
                    ? userPersonalDetailsRepository.findAll(EmployeeReportSpecification.getSpecification(search), pageable)
                    : userPersonalDetailsRepository.findAll(EmployeeReportSpecification.getSpecification(), pageable);

            long totalElements = Objects.nonNull(search)
                    ? userPersonalDetailsRepository.count(EmployeeReportSpecification.getSpecification(search))
                    : userPersonalDetailsRepository.count(EmployeeReportSpecification.getSpecification());

            List<EmployeeDetailsResponseDTO> rows = employeePage.stream()
                    .map(employeeDetailsMapperEntityToDto::mapEmployeeDetails)
                    .toList();

            PagingResult<EmployeeDetailsResponseDTO> result = new PagingResult<>(rows, rows.size(), totalElements);

            auditLogService.log(PAGE_EMPLOYEE_REPORT, WebTask.SEARCH.name(),
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
    public ResponseEntity<ApiResponse<Object>> view(EmployeeDetailsRequestDTO employeeDetailsRequestDTO, Locale locale) {
        try {
            log.info("Employee report view {}", employeeDetailsRequestDTO);
            return userPersonalDetailsRepository.findById(employeeDetailsRequestDTO.getId()).map(userPersonalDetails -> {
                EmployeeDetailsResponseDTO employeeDetailsResponseDTO = employeeDetailsMapperEntityToDto.mapEmployeeDetails(userPersonalDetails);
                auditLogService.log(PAGE_EMPLOYEE_REPORT, WebTask.VIEW.name(),
                        AuditTask.VIEW_DATA.getDescription(), employeeDetailsRequestDTO.getIp(),
                        employeeDetailsRequestDTO.getUserAgent(), gson.toJson(employeeDetailsResponseDTO), null, employeeDetailsRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) employeeDetailsResponseDTO,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_RETRIEVE_SUCCESSFULLY, null, locale)));
            }).orElseGet(() -> {
                log.info("Employee report not found {}", employeeDetailsRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1043,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND,
                                new Object[]{employeeDetailsRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<EmployeeReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Employee report export {}", paginationRequest);
            EmployeeReportSearchDTO search = paginationRequest.getSearch();

            List<UserPersonalDetails> employees = Objects.nonNull(search)
                    ? userPersonalDetailsRepository.findAll(EmployeeReportSpecification.getSpecification(search))
                    : userPersonalDetailsRepository.findAll(EmployeeReportSpecification.getSpecification());

            byte[] excelBytes = buildExcel(employees);
            auditLogService.log(PAGE_EMPLOYEE_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(search), null, paginationRequest.getUsername());

            String fileName = "employee-list-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export employee report", e);
            throw e;
        }
    }

    private void normalizeSortColumn(PaginationRequest<EmployeeReportSearchDTO> paginationRequest) {
        String sortColumn = paginationRequest.getSortColumn();
        if (sortColumn == null || sortColumn.isBlank()) {
            return;
        }
        switch (sortColumn) {
            case "permanentDate" -> paginationRequest.setSortColumn("userCompanyDetails.permanentDate");
            case "company" -> paginationRequest.setSortColumn("userCompanyDetails.companyTypes.code");
            case "staffCategory" -> paginationRequest.setSortColumn("userCompanyDetails.staffCategories.code");
            case "facility" -> paginationRequest.setSortColumn("userCompanyDetails.facility");
            case "status" -> paginationRequest.setSortColumn("userStatus");
            case "epf" -> paginationRequest.setSortColumn("epfNo");
            default -> {
                // Keep provided sort column.
            }
        }
    }

    private byte[] buildExcel(List<UserPersonalDetails> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employee Report");

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
            sheet.setColumnWidth(2, 12 * 256);
            sheet.setColumnWidth(3, 10 * 256);
            sheet.setColumnWidth(4, 14 * 256);
            sheet.setColumnWidth(5, 18 * 256);
            sheet.setColumnWidth(6, 18 * 256);
            sheet.setColumnWidth(7, 16 * 256);
            sheet.setColumnWidth(8, 26 * 256);
            sheet.setColumnWidth(9, 14 * 256);
            sheet.setColumnWidth(10, 12 * 256);
            sheet.setColumnWidth(11, 14 * 256);
            sheet.setColumnWidth(12, 12 * 256);
            sheet.setColumnWidth(13, 12 * 256);
            sheet.setColumnWidth(14, 22 * 256);
            sheet.setColumnWidth(15, 22 * 256);
            sheet.setColumnWidth(16, 16 * 256);
            sheet.setColumnWidth(17, 22 * 256);
            sheet.setColumnWidth(18, 22 * 256);
            sheet.setColumnWidth(19, 22 * 256);
            sheet.setColumnWidth(20, 18 * 256);
            sheet.setColumnWidth(21, 18 * 256);
            sheet.setColumnWidth(22, 16 * 256);
            sheet.setColumnWidth(23, 18 * 256);
            sheet.setColumnWidth(24, 14 * 256);
            sheet.setColumnWidth(25, 14 * 256);
            sheet.setColumnWidth(26, 12 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("Employee List Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 26));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Employee ID", headerStyle);
            createStringCell(row, 2, "EPF", headerStyle);
            createStringCell(row, 3, "Title", headerStyle);
            createStringCell(row, 4, "Initials", headerStyle);
            createStringCell(row, 5, "First Name", headerStyle);
            createStringCell(row, 6, "Last Name", headerStyle);
            createStringCell(row, 7, "NIC", headerStyle);
            createStringCell(row, 8, "Email", headerStyle);
            createStringCell(row, 9, "Mobile", headerStyle);
            createStringCell(row, 10, "Gender", headerStyle);
            createStringCell(row, 11, "Marital Status", headerStyle);
            createStringCell(row, 12, "DOB", headerStyle);
            createStringCell(row, 13, "Address No", headerStyle);
            createStringCell(row, 14, "Address Line 1", headerStyle);
            createStringCell(row, 15, "Address Line 2", headerStyle);
            createStringCell(row, 16, "City", headerStyle);
            createStringCell(row, 17, "Company", headerStyle);
            createStringCell(row, 18, "Payment Company", headerStyle);
            createStringCell(row, 19, "Staff Category", headerStyle);
            createStringCell(row, 20, "Staff Type", headerStyle);
            createStringCell(row, 21, "Designation", headerStyle);
            createStringCell(row, 22, "Facility", headerStyle);
            createStringCell(row, 23, "Insurance Policy", headerStyle);
            createStringCell(row, 24, "Permanent Date", headerStyle);
            createStringCell(row, 25, "Terminate Date", headerStyle);
            createStringCell(row, 26, "Status", headerStyle);

            int lineNo = 1;
            for (UserPersonalDetails rowDTO : rows) {
                row = sheet.createRow(rowIndex++);
                UserCompanyDetails companyDetails = rowDTO.getUserCompanyDetails();
                String companyCode = companyDetails != null && companyDetails.getCompanyTypes() != null
                        ? companyDetails.getCompanyTypes().getCode() : "";
                String companyDescription = companyDetails != null && companyDetails.getCompanyTypes() != null
                        ? companyDetails.getCompanyTypes().getDescription() : "";
                String paymentCompanyCode = companyDetails != null && companyDetails.getPaymentCompany() != null
                        ? companyDetails.getPaymentCompany().getCode() : "";
                String paymentCompanyDescription = companyDetails != null && companyDetails.getPaymentCompany() != null
                        ? companyDetails.getPaymentCompany().getDescription() : "";
                String staffCategoryCode = companyDetails != null && companyDetails.getStaffCategories() != null
                        ? companyDetails.getStaffCategories().getCode() : "";
                String staffCategoryDescription = companyDetails != null && companyDetails.getStaffCategories() != null
                        ? companyDetails.getStaffCategories().getDescription() : "";
                String staffTypeCode = companyDetails != null && companyDetails.getStaffTypes() != null
                        ? companyDetails.getStaffTypes().getCode() : "";
                String staffTypeDescription = companyDetails != null && companyDetails.getStaffTypes() != null
                        ? companyDetails.getStaffTypes().getDescription() : "";
                String facilityCode = companyDetails != null && companyDetails.getFacility() != null
                        ? companyDetails.getFacility().name() : "";
                String facilityDescription = companyDetails != null && companyDetails.getFacility() != null
                        ? companyDetails.getFacility().getDescription() : "";
                String insuranceCode = companyDetails != null && companyDetails.getInsurancePolicy() != null
                        ? companyDetails.getInsurancePolicy().getCode() : "";
                String insuranceDescription = companyDetails != null && companyDetails.getInsurancePolicy() != null
                        ? companyDetails.getInsurancePolicy().getDescription() : "";

                String titleCode = rowDTO.getTitle() != null ? rowDTO.getTitle().name() : "";
                String titleDescription = rowDTO.getTitle() != null ? rowDTO.getTitle().getDescription() : "";
                String genderCode = rowDTO.getGender() != null ? rowDTO.getGender().name() : "";
                String genderDescription = rowDTO.getGender() != null ? rowDTO.getGender().getDescription() : "";
                String maritalCode = rowDTO.getMaritalStatus() != null ? rowDTO.getMaritalStatus().name() : "";
                String maritalDescription = rowDTO.getMaritalStatus() != null ? rowDTO.getMaritalStatus().getDescription() : "";
                String statusCode = rowDTO.getUserStatus() != null ? rowDTO.getUserStatus().name() : "";
                String statusDescription = rowDTO.getUserStatus() != null ? rowDTO.getUserStatus().getDescription() : "";

                String streetNo = rowDTO.getUserAddress() != null ? rowDTO.getUserAddress().getStreetNo() : "";
                String street1 = rowDTO.getUserAddress() != null ? rowDTO.getUserAddress().getStreet1() : "";
                String street2 = rowDTO.getUserAddress() != null ? rowDTO.getUserAddress().getStreet2() : "";
                String city = rowDTO.getUserAddress() != null ? rowDTO.getUserAddress().getCity() : "";

                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, String.valueOf(rowDTO.getId()), dataStyle);
                createStringCell(row, 2, rowDTO.getEpfNo(), dataStyle);
                createStringCell(row, 3, buildDisplay(titleCode, titleDescription), dataStyle);
                createStringCell(row, 4, rowDTO.getInitials(), dataStyle);
                createStringCell(row, 5, rowDTO.getFirstName(), dataStyle);
                createStringCell(row, 6, rowDTO.getLastName(), dataStyle);
                createStringCell(row, 7, rowDTO.getNic(), dataStyle);
                createStringCell(row, 8, rowDTO.getEmail(), dataStyle);
                createStringCell(row, 9, rowDTO.getMobileNo(), dataStyle);
                createStringCell(row, 10, buildDisplay(genderCode, genderDescription), dataStyle);
                createStringCell(row, 11, buildDisplay(maritalCode, maritalDescription), dataStyle);
                createStringCell(row, 12, formatDate(rowDTO.getDob()), dataStyle);
                createStringCell(row, 13, streetNo, dataStyle);
                createStringCell(row, 14, street1, dataStyle);
                createStringCell(row, 15, street2, dataStyle);
                createStringCell(row, 16, city, dataStyle);
                createStringCell(row, 17, buildDisplay(companyCode, companyDescription), dataStyle);
                createStringCell(row, 18, buildDisplay(paymentCompanyCode, paymentCompanyDescription), dataStyle);
                createStringCell(row, 19, buildDisplay(staffCategoryCode, staffCategoryDescription), dataStyle);
                createStringCell(row, 20, buildDisplay(staffTypeCode, staffTypeDescription), dataStyle);
                createStringCell(row, 21, companyDetails != null ? companyDetails.getDesignation() : "", dataStyle);
                createStringCell(row, 22, buildDisplay(facilityCode, facilityDescription), dataStyle);
                createStringCell(row, 23, buildDisplay(insuranceCode, insuranceDescription), dataStyle);
                createStringCell(row, 24, formatDate(companyDetails != null ? companyDetails.getPermanentDate() : null), dataStyle);
                createStringCell(row, 25, formatDate(companyDetails != null ? companyDetails.getTerminateDate() : null), dataStyle);
                createStringCell(row, 26, buildDisplay(statusCode, statusDescription), dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate employee report excel", e);
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

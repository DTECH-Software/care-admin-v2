package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DependentRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.DependentDetailsResponseDTO;
import com.dtech.admin.dto.response.DependentReportRowDTO;
import com.dtech.admin.dto.search.DependentReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.DependentCategory;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.entityToDto.DependentDetailsMapperEntityToDto;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.ClaimDependentsRepository;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.DependentReportService;
import com.dtech.admin.specifications.DependentReportSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.DateTimeUtil;
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
public class DependentReportServiceImpl implements DependentReportService {

    private static final String PAGE_DEPENDENT_REPORT = WebPage.RPDL.name();

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
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private final DependentDetailsMapperEntityToDto dependentDetailsMapperEntityToDto;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Dependent report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_DEPENDENT_REPORT);

            List<SimpleBaseDTO> dependentCategory = Arrays.stream(DependentCategory.values())
                    .map(val -> new SimpleBaseDTO(val.name(), val.getDescription()))
                    .toList();

            List<SimpleBaseDTO> company = companyTypeRepository.findAllByStatus(com.dtech.admin.enums.Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> staffCategories = staffCategoriesRepository.findAllByStatus(com.dtech.admin.enums.Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("company", company);
            responseMap.put("dependentCategory", dependentCategory);
            responseMap.put("staffCategories", staffCategories);

            auditLogService.log(PAGE_DEPENDENT_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS,
                            new Object[]{PAGE_DEPENDENT_REPORT}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<DependentReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Dependent report filter list {}", paginationRequest);
            normalizeSortColumn(paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            DependentReportSearchDTO search = paginationRequest.getSearch();
            Page<ClaimsDependents> dependentsPage = Objects.nonNull(search)
                    ? claimDependentsRepository.findAll(DependentReportSpecification.getSpecification(search), pageable)
                    : claimDependentsRepository.findAll(DependentReportSpecification.getSpecification(), pageable);

            long totalElements = Objects.nonNull(search)
                    ? claimDependentsRepository.count(DependentReportSpecification.getSpecification(search))
                    : claimDependentsRepository.count(DependentReportSpecification.getSpecification());

            List<DependentDetailsResponseDTO> rows = dependentsPage.stream()
                    .map(dependentDetailsMapperEntityToDto::mapDependentDetails)
                    .map(this::stripDependentDocuments)
                    .toList();

            PagingResult<DependentDetailsResponseDTO> result = new PagingResult<>(rows, rows.size(), totalElements);

            auditLogService.log(PAGE_DEPENDENT_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(rows), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.DEPENDENT_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(DependentRequestDTO dependentRequestDTO, Locale locale) {
        try {
            log.info("Dependent report view {}", dependentRequestDTO);
            return claimDependentsRepository.findById(dependentRequestDTO.getId()).map(dependent -> {
                DependentDetailsResponseDTO responseDTO = dependentDetailsMapperEntityToDto.mapDependentDetails(dependent);
                responseDTO = stripDependentDocuments(responseDTO);
                auditLogService.log(PAGE_DEPENDENT_REPORT, WebTask.VIEW.name(),
                        AuditTask.VIEW_DATA.getDescription(), dependentRequestDTO.getIp(),
                        dependentRequestDTO.getUserAgent(), gson.toJson(responseDTO), null, dependentRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                        messageSource.getMessage(ResponseMessageUtil.DEPENDENT_DETAILS_RETRIEVE_SUCCESSFULLY, null, locale)));
            }).orElseGet(() -> {
                log.info("Dependent report not found {}", dependentRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1048,
                        messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_FOUND,
                                new Object[]{dependentRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<DependentReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Dependent report export {}", paginationRequest);
            DependentReportSearchDTO search = paginationRequest.getSearch();

            List<ClaimsDependents> dependents = Objects.nonNull(search)
                    ? claimDependentsRepository.findAll(DependentReportSpecification.getSpecification(search))
                    : claimDependentsRepository.findAll(DependentReportSpecification.getSpecification());

            List<DependentReportRowDTO> rows = dependents.stream()
                    .map(this::mapRow)
                    .toList();

            byte[] excelBytes = buildExcel(rows);
            auditLogService.log(PAGE_DEPENDENT_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(search), null, paginationRequest.getUsername());

            String fileName = "dependent-list-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export dependent report", e);
            throw e;
        }
    }

    private DependentDetailsResponseDTO stripDependentDocuments(DependentDetailsResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        dto.setAttachment(null);
        if (dto.getApplicationUser() != null && dto.getApplicationUser().getUserPersonalDetails() != null) {
            dto.getApplicationUser().getUserPersonalDetails().setBirthImg(null);
            dto.getApplicationUser().getUserPersonalDetails().setMaritalStatusDocument(null);
        }
        return dto;
    }

    private DependentReportRowDTO mapRow(ClaimsDependents dependent) {
        DependentReportRowDTO dto = new DependentReportRowDTO();
        dto.setDependentId(dependent.getId());
        if (dependent.getDependentCategory() != null) {
            dto.setDependentCategory(dependent.getDependentCategory().name());
            dto.setDependentCategoryDescription(dependent.getDependentCategory().getDescription());
        }
        if (dependent.getRelationCategory() != null) {
            dto.setRelationCategory(dependent.getRelationCategory().name());
            dto.setRelationCategoryDescription(dependent.getRelationCategory().getDescription());
        }
        dto.setInitials(dependent.getInitials());
        dto.setFirstName(dependent.getFirstName());
        dto.setLastName(dependent.getLastName());
        if (dependent.getGender() != null) {
            dto.setGender(dependent.getGender().name());
            dto.setGenderDescription(dependent.getGender().getDescription());
        }
        dto.setDob(dependent.getDob());
        dto.setAge(resolveAge(dependent.getDob()));
        dto.setNic(dependent.getNic());
        dto.setJobTitle(dependent.getJobTitle());
        if (dependent.getEligibleFacility() != null) {
            dto.setEligibleFacility(dependent.getEligibleFacility().name());
            dto.setEligibleFacilityDescription(dependent.getEligibleFacility().getDescription());
        }
        if (dependent.getStatus() != null) {
            dto.setStatus(dependent.getStatus().name());
            dto.setStatusDescription(dependent.getStatus().getDescription());
        }
        dto.setLiveStatus(dependent.getLiveStatus());
        dto.setApprovedDate(dependent.getApprovedDate());
        dto.setApprovedUser(dependent.getApprovedUser());
        dto.setRemark(dependent.getRemark());

        ApplicationUser user = dependent.getApplicationUser();
        if (user != null) {
            UserPersonalDetails details = user.getUserPersonalDetails();
            if (details != null) {
                dto.setEmployeeId(details.getId());
                dto.setEpf(details.getEpfNo());
                dto.setEmployeeName(buildEmployeeName(details));

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
                }
            }
        }

        return dto;
    }

    private void normalizeSortColumn(PaginationRequest<DependentReportSearchDTO> paginationRequest) {
        String sortColumn = paginationRequest.getSortColumn();
        if (sortColumn == null || sortColumn.isBlank()) {
            return;
        }
        switch (sortColumn) {
            case "company" -> paginationRequest.setSortColumn("applicationUser.userPersonalDetails.userCompanyDetails.companyTypes.code");
            case "staffCategory" -> paginationRequest.setSortColumn("applicationUser.userPersonalDetails.userCompanyDetails.staffCategories.code");
            case "employeeName" -> paginationRequest.setSortColumn("applicationUser.userPersonalDetails.firstName");
            case "epf" -> paginationRequest.setSortColumn("applicationUser.userPersonalDetails.epfNo");
            case "dependentCategory" -> paginationRequest.setSortColumn("dependentCategory");
            case "relationCategory" -> paginationRequest.setSortColumn("relationCategory");
            case "status" -> paginationRequest.setSortColumn("status");
            case "approvedDate" -> paginationRequest.setSortColumn("approvedDate");
            case "dob" -> paginationRequest.setSortColumn("dob");
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

    private int resolveAge(Date dob) {
        if (dob == null) {
            return 0;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return DateTimeUtil.getAge(formatter.format(dob));
    }

    private byte[] buildExcel(List<DependentReportRowDTO> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Dependent Report");

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
            sheet.setColumnWidth(1, 10 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            sheet.setColumnWidth(3, 18 * 256);
            sheet.setColumnWidth(4, 12 * 256);
            sheet.setColumnWidth(5, 16 * 256);
            sheet.setColumnWidth(6, 16 * 256);
            sheet.setColumnWidth(7, 14 * 256);
            sheet.setColumnWidth(8, 12 * 256);
            sheet.setColumnWidth(9, 8 * 256);
            sheet.setColumnWidth(10, 14 * 256);
            sheet.setColumnWidth(11, 18 * 256);
            sheet.setColumnWidth(12, 18 * 256);
            sheet.setColumnWidth(13, 14 * 256);
            sheet.setColumnWidth(14, 10 * 256);
            sheet.setColumnWidth(15, 16 * 256);
            sheet.setColumnWidth(16, 16 * 256);
            sheet.setColumnWidth(17, 22 * 256);
            sheet.setColumnWidth(18, 12 * 256);
            sheet.setColumnWidth(19, 12 * 256);
            sheet.setColumnWidth(20, 18 * 256);
            sheet.setColumnWidth(21, 20 * 256);
            sheet.setColumnWidth(22, 18 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("Dependent List Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 22));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Dependent ID", headerStyle);
            createStringCell(row, 2, "Dependent Category", headerStyle);
            createStringCell(row, 3, "Relation Category", headerStyle);
            createStringCell(row, 4, "Initials", headerStyle);
            createStringCell(row, 5, "First Name", headerStyle);
            createStringCell(row, 6, "Last Name", headerStyle);
            createStringCell(row, 7, "Gender", headerStyle);
            createStringCell(row, 8, "DOB", headerStyle);
            createStringCell(row, 9, "Age", headerStyle);
            createStringCell(row, 10, "NIC", headerStyle);
            createStringCell(row, 11, "Job Title", headerStyle);
            createStringCell(row, 12, "Eligible Facility", headerStyle);
            createStringCell(row, 13, "Status", headerStyle);
            createStringCell(row, 14, "Live Status", headerStyle);
            createStringCell(row, 15, "Approved Date", headerStyle);
            createStringCell(row, 16, "Approved User", headerStyle);
            createStringCell(row, 17, "Remark", headerStyle);
            createStringCell(row, 18, "Employee ID", headerStyle);
            createStringCell(row, 19, "EPF", headerStyle);
            createStringCell(row, 20, "Employee Name", headerStyle);
            createStringCell(row, 21, "Company", headerStyle);
            createStringCell(row, 22, "Staff Category", headerStyle);

            int lineNo = 1;
            for (DependentReportRowDTO rowDTO : rows) {
                row = sheet.createRow(rowIndex++);
                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, rowDTO.getDependentId() != null ? rowDTO.getDependentId().toString() : "", dataStyle);
                createStringCell(row, 2, descriptionOnly(rowDTO.getDependentCategory(), rowDTO.getDependentCategoryDescription()), dataStyle);
                createStringCell(row, 3, descriptionOnly(rowDTO.getRelationCategory(), rowDTO.getRelationCategoryDescription()), dataStyle);
                createStringCell(row, 4, rowDTO.getInitials(), dataStyle);
                createStringCell(row, 5, rowDTO.getFirstName(), dataStyle);
                createStringCell(row, 6, rowDTO.getLastName(), dataStyle);
                createStringCell(row, 7, descriptionOnly(rowDTO.getGender(), rowDTO.getGenderDescription()), dataStyle);
                createStringCell(row, 8, formatDate(rowDTO.getDob()), dataStyle);
                createStringCell(row, 9, String.valueOf(rowDTO.getAge()), dataStyle);
                createStringCell(row, 10, rowDTO.getNic(), dataStyle);
                createStringCell(row, 11, rowDTO.getJobTitle(), dataStyle);
                createStringCell(row, 12, descriptionOnly(rowDTO.getEligibleFacility(), rowDTO.getEligibleFacilityDescription()), dataStyle);
                createStringCell(row, 13, descriptionOnly(rowDTO.getStatus(), rowDTO.getStatusDescription()), dataStyle);
                createStringCell(row, 14, rowDTO.getLiveStatus() != null ? rowDTO.getLiveStatus().toString() : "", dataStyle);
                createStringCell(row, 15, formatDate(rowDTO.getApprovedDate()), dataStyle);
                createStringCell(row, 16, rowDTO.getApprovedUser(), dataStyle);
                createStringCell(row, 17, rowDTO.getRemark(), dataStyle);
                createStringCell(row, 18, rowDTO.getEmployeeId() != null ? rowDTO.getEmployeeId().toString() : "", dataStyle);
                createStringCell(row, 19, rowDTO.getEpf(), dataStyle);
                createStringCell(row, 20, rowDTO.getEmployeeName(), dataStyle);
                createStringCell(row, 21, buildDisplay(rowDTO.getCompanyCode(), rowDTO.getCompanyDescription()), dataStyle);
                createStringCell(row, 22, buildDisplay(rowDTO.getStaffCategoryCode(), rowDTO.getStaffCategoryDescription()), dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dependent report excel", e);
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

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }
}

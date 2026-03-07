package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DdfClaimReportRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.DeathRequestResponseDTO;
import com.dtech.admin.dto.response.DdfClaimReportRowDTO;
import com.dtech.admin.dto.search.DdfClaimReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.entityToDto.DeathApprovalEntityToDto;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.DeathClaimRequest;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.DeathClaimRequestRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.DdfClaimReportService;
import com.dtech.admin.specifications.DdfClaimReportSpecification;
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
public class DdfClaimReportServiceImpl implements DdfClaimReportService {

    private static final String PAGE_DDF_CLAIM_REPORT = WebPage.RPDF.name();

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
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final DeathApprovalEntityToDto deathApprovalEntityToDto;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("DDF claim report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_DDF_CLAIM_REPORT);

            List<SimpleBaseDTO> status = List.of(
                    new SimpleBaseDTO(Workflow.APPROVED.name(), Workflow.APPROVED.getDescription()),
                    new SimpleBaseDTO(Workflow.REJECTED.name(), Workflow.REJECTED.getDescription())
            );

            List<SimpleBaseDTO> company = companyTypeRepository.findAllByStatus(com.dtech.admin.enums.Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("status", status);
            responseMap.put("company", company);

            auditLogService.log(PAGE_DDF_CLAIM_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS,
                            new Object[]{PAGE_DDF_CLAIM_REPORT}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<DdfClaimReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("DDF claim report filter list {}", paginationRequest);
            normalizeSortColumn(paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            DdfClaimReportSearchDTO search = paginationRequest.getSearch();
            Page<DeathClaimRequest> claimsPage = Objects.nonNull(search)
                    ? deathClaimRequestRepository.findAll(DdfClaimReportSpecification.getSpecification(search), pageable)
                    : deathClaimRequestRepository.findAll(DdfClaimReportSpecification.getSpecification(), pageable);

            long totalElements = Objects.nonNull(search)
                    ? deathClaimRequestRepository.count(DdfClaimReportSpecification.getSpecification(search))
                    : deathClaimRequestRepository.count(DdfClaimReportSpecification.getSpecification());

            List<DeathRequestResponseDTO> rows = claimsPage.stream()
                    .map(claim -> {
                        DeathRequestResponseDTO dto = deathApprovalEntityToDto.mapClaimsApproval(claim, false);
                        stripDocuments(dto);
                        return dto;
                    })
                    .toList();

            PagingResult<DeathRequestResponseDTO> result = new PagingResult<>(rows, rows.size(), totalElements);

            auditLogService.log(PAGE_DDF_CLAIM_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(rows), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.DDF_CLAIM_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(DdfClaimReportRequestDTO requestDTO, Locale locale) {
        try {
            log.info("DDF claim report view {}", requestDTO);
            return deathClaimRequestRepository.findById(requestDTO.getId()).map(claim -> {
                DeathRequestResponseDTO row = deathApprovalEntityToDto.mapClaimsApproval(claim, false);
                stripDocuments(row);
                auditLogService.log(PAGE_DDF_CLAIM_REPORT, WebTask.VIEW.name(),
                        AuditTask.VIEW_DATA.getDescription(), requestDTO.getIp(),
                        requestDTO.getUserAgent(), gson.toJson(row), null, requestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) row,
                        messageSource.getMessage(ResponseMessageUtil.DDF_CLAIM_REPORT_VIEW_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("DDF claim report not found {}", requestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1053,
                        messageSource.getMessage(ResponseMessageUtil.DDF_CLAIM_REPORT_NOT_FOUND,
                                new Object[]{requestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<DdfClaimReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("DDF claim report export {}", paginationRequest);
            DdfClaimReportSearchDTO search = paginationRequest.getSearch();

            List<DeathClaimRequest> claims = Objects.nonNull(search)
                    ? deathClaimRequestRepository.findAll(DdfClaimReportSpecification.getSpecification(search))
                    : deathClaimRequestRepository.findAll(DdfClaimReportSpecification.getSpecification());

            List<DdfClaimReportRowDTO> rows = claims.stream()
                    .map(this::mapRow)
                    .toList();

            byte[] excelBytes = buildExcel(rows);
            auditLogService.log(PAGE_DDF_CLAIM_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(search), null, paginationRequest.getUsername());

            String fileName = "ddf-claim-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export DDF claim report", e);
            throw e;
        }
    }

    private DdfClaimReportRowDTO mapRow(DeathClaimRequest claim) {
        DdfClaimReportRowDTO dto = new DdfClaimReportRowDTO();
        if (claim.getEmployee() != null && claim.getEmployee().getUserPersonalDetails() != null) {
            UserPersonalDetails details = claim.getEmployee().getUserPersonalDetails();
            dto.setEpf(details.getEpfNo());
            dto.setEmployeeName(buildEmployeeName(details));

            UserCompanyDetails companyDetails = details.getUserCompanyDetails();
            if (companyDetails != null && companyDetails.getCompanyTypes() != null) {
                dto.setCompanyName(companyDetails.getCompanyTypes().getDescription());
            }
        }

        ClaimsDependents dependent = claim.getClaimsDependents();
        if (dependent != null) {
            dto.setRelationName(buildDependentName(dependent));
            if (dependent.getRelationCategory() != null) {
                dto.setRelation(dependent.getRelationCategory().name());
                dto.setRelationDescription(dependent.getRelationCategory().getDescription());
            }
        }

        if (claim.getRequestStatus() != null) {
            dto.setStatus(claim.getRequestStatus().name());
            dto.setStatusDescription(claim.getRequestStatus().getDescription());
        }
        dto.setDeathDate(claim.getDeathDate());
        dto.setCreatedDate(claim.getCreatedDate());
        dto.setApprovedAmount(claim.getApprovedAmount());
        return dto;
    }

    private void stripDocuments(DeathRequestResponseDTO dto) {
        if (dto == null) {
            return;
        }
        dto.setDocuments(null);
        if (dto.getEmployee() != null && dto.getEmployee().getUserPersonalDetails() != null) {
            dto.getEmployee().getUserPersonalDetails().setBirthImg(null);
            dto.getEmployee().getUserPersonalDetails().setMaritalStatusDocument(null);
            if (dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails() != null) {
                dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails().setPromoDoc(null);
            }
        }
    }

    private void normalizeSortColumn(PaginationRequest<DdfClaimReportSearchDTO> paginationRequest) {
        String sortColumn = paginationRequest.getSortColumn();
        if (sortColumn == null || sortColumn.isBlank()) {
            return;
        }
        switch (sortColumn) {
            case "createdDate" -> paginationRequest.setSortColumn("createdDate");
            case "deathDate" -> paginationRequest.setSortColumn("deathDate");
            case "status" -> paginationRequest.setSortColumn("requestStatus");
            case "company" -> paginationRequest.setSortColumn("employee.userPersonalDetails.userCompanyDetails.companyTypes.code");
            case "epf" -> paginationRequest.setSortColumn("employee.userPersonalDetails.epfNo");
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

    private String buildDependentName(ClaimsDependents dependent) {
        String firstName = dependent.getFirstName() != null ? dependent.getFirstName().trim() : "";
        String lastName = dependent.getLastName() != null ? dependent.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? dependent.getInitials() : fullName;
    }

    private byte[] buildExcel(List<DdfClaimReportRowDTO> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DDF Claim Report");

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
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 22 * 256);
            sheet.setColumnWidth(4, 20 * 256);
            sheet.setColumnWidth(5, 16 * 256);
            sheet.setColumnWidth(6, 14 * 256);
            sheet.setColumnWidth(7, 14 * 256);
            sheet.setColumnWidth(8, 14 * 256);
            sheet.setColumnWidth(9, 18 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("DDF Claim Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 9));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "EPF No", headerStyle);
            createStringCell(row, 2, "Employee Name", headerStyle);
            createStringCell(row, 3, "Company Name", headerStyle);
            createStringCell(row, 4, "Relation Name", headerStyle);
            createStringCell(row, 5, "Relation", headerStyle);
            createStringCell(row, 6, "Status", headerStyle);
            createStringCell(row, 7, "Approved Amount", headerStyle);
            createStringCell(row, 8, "Death Date", headerStyle);
            createStringCell(row, 9, "Enter Date", headerStyle);

            int lineNo = 1;
            for (DdfClaimReportRowDTO rowDTO : rows) {
                row = sheet.createRow(rowIndex++);
                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, rowDTO.getEpf(), dataStyle);
                createStringCell(row, 2, rowDTO.getEmployeeName(), dataStyle);
                createStringCell(row, 3, rowDTO.getCompanyName(), dataStyle);
                createStringCell(row, 4, rowDTO.getRelationName(), dataStyle);
                createStringCell(row, 5, buildDisplay(rowDTO.getRelation(), rowDTO.getRelationDescription()), dataStyle);
                createStringCell(row, 6, buildDisplay(rowDTO.getStatus(), rowDTO.getStatusDescription()), dataStyle);
                createStringCell(row, 7, rowDTO.getApprovedAmount() != null ? rowDTO.getApprovedAmount().toString() : "", dataStyle);
                createStringCell(row, 8, formatDate(rowDTO.getDeathDate()), dataStyle);
                createStringCell(row, 9, formatDate(rowDTO.getCreatedDate()), dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DDF claim report excel", e);
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

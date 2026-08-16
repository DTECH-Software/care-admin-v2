package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DdfReportRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.DdfReportRowDTO;
import com.dtech.admin.dto.response.DeathRequestResponseDTO;
import com.dtech.admin.dto.search.DdfReportSearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.RelationCategory;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.entityToDto.DeathApprovalEntityToDto;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.DeathClaimRequest;
import com.dtech.admin.model.PaymentAdviceDeathClaim;
import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.DeathClaimRequestRepository;
import com.dtech.admin.repository.PaymentAdviceDeathClaimRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.CompanyAccessService;
import com.dtech.admin.service.DdfReportService;
import com.dtech.admin.specifications.CompanyScopeSpecification;
import com.dtech.admin.specifications.DdfReportSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class DdfReportServiceImpl implements DdfReportService {

    private static final String PAGE_DDF_REPORT = WebPage.RPRT_DDFR.name();

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
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final PaymentAdviceDeathClaimRepository paymentAdviceDeathClaimRepository;

    @Autowired
    private final DeathApprovalEntityToDto deathApprovalEntityToDto;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("DDF report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_DDF_REPORT);

            List<SimpleBaseDTO> status = List.of(
                    new SimpleBaseDTO(Workflow.APPROVED.name(), Workflow.APPROVED.getDescription()),
                    new SimpleBaseDTO(Workflow.REJECTED.name(), Workflow.REJECTED.getDescription())
            );

            List<SimpleBaseDTO> paymentAdviceStatus = List.of(
                    new SimpleBaseDTO("GENERATED", "Generated"),
                    new SimpleBaseDTO("NOT_GENERATED", "Not Generated")
            );

            List<SimpleBaseDTO> company = companyAccessService.activeCompanies(channelRequestDTO.getUsername())
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();


            List<SimpleBaseDTO> relationCategories = java.util.Arrays.stream(RelationCategory.values())
                    .map(val -> new SimpleBaseDTO(val.name(), val.getDescription()))
                    .toList();

            responseMap.put("privileges", privileges);
            responseMap.put("status", status);
            responseMap.put("paymentAdviceStatus", paymentAdviceStatus);
            responseMap.put("company", company);
            responseMap.put("relationCategory", relationCategories);

            auditLogService.log(PAGE_DDF_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.DDF_REPORT_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load DDF report reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<DdfReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("DDF report filter list {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            DdfReportSearchDTO search = paginationRequest.getSearch();
            Specification<DeathClaimRequest> specification = scopedSpecification(search, paginationRequest.getUsername());
            Page<DeathClaimRequest> claimsPage = deathClaimRequestRepository.findAll(specification, pageable);
            long total = deathClaimRequestRepository.count(specification);

            Set<Long> generatedIds = resolveGeneratedClaimIds(claimsPage.getContent());
            Map<Long, PaymentAdviceDeathClaim> adviceByClaimId = resolveAdviceByClaimId(claimsPage.getContent());

            List<DdfReportRowDTO> rows = claimsPage.stream()
                    .map(claim -> mapRow(claim, generatedIds.contains(claim.getId()), adviceByClaimId.get(claim.getId())))
                    .toList();

            PagingResult<DdfReportRowDTO> result = new PagingResult<>(rows, rows.size(), total);

            auditLogService.log(PAGE_DDF_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(rows), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.DDF_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter DDF report", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(DdfReportRequestDTO requestDTO, Locale locale) {
        try {
            log.info("DDF report view {}", requestDTO);
            return deathClaimRequestRepository.findById(requestDTO.getId())
                    .filter(claim -> canAccess(claim, requestDTO.getUsername())).map(claim -> {
                DeathRequestResponseDTO row = deathApprovalEntityToDto.mapClaimsApproval(claim, false);
                stripDocuments(row);

                boolean generated = paymentAdviceDeathClaimRepository.existsByDeathClaim(claim);
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("claim", row);
                responseMap.put("paymentAdviceGenerated", generated);
                responseMap.put("paymentAdviceStatusDescription", generated ? "Generated" : "Not Generated");

                auditLogService.log(PAGE_DDF_REPORT, WebTask.VIEW.name(),
                        AuditTask.VIEW_DATA.getDescription(), requestDTO.getIp(),
                        requestDTO.getUserAgent(), gson.toJson(responseMap), null, requestDTO.getUsername());

                return ResponseEntity.ok().body(responseUtil.success((Object) responseMap,
                        messageSource.getMessage(ResponseMessageUtil.DDF_REPORT_VIEW_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("DDF report claim not found {}", requestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1053,
                        messageSource.getMessage(ResponseMessageUtil.DDF_REPORT_NOT_FOUND,
                                new Object[]{requestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error("Failed to view DDF report", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> export(PaginationRequest<DdfReportSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("DDF report export {}", paginationRequest);
            DdfReportSearchDTO search = paginationRequest.getSearch();

            List<DeathClaimRequest> claims = deathClaimRequestRepository.findAll(
                    scopedSpecification(search, paginationRequest.getUsername()));

            Set<Long> generatedIds = resolveGeneratedClaimIds(claims);
            Map<Long, PaymentAdviceDeathClaim> adviceByClaimId = resolveAdviceByClaimId(claims);

            List<DdfReportRowDTO> rows = claims.stream()
                    .map(claim -> mapRow(claim, generatedIds.contains(claim.getId()), adviceByClaimId.get(claim.getId())))
                    .toList();

            byte[] excelBytes = buildExcel(rows);
            auditLogService.log(PAGE_DDF_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(search), null, paginationRequest.getUsername());

            String fileName = "ddf-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export DDF report", e);
            throw e;
        }
    }

    private Set<Long> resolveGeneratedClaimIds(List<DeathClaimRequest> claims) {
        if (claims == null || claims.isEmpty()) {
            return Set.of();
        }
        List<PaymentAdviceDeathClaim> adviceClaims = paymentAdviceDeathClaimRepository.findAllByDeathClaimIn(claims);
        return adviceClaims.stream()
                .map(advice -> advice.getDeathClaim().getId())
                .collect(Collectors.toSet());
    }

    private Specification<DeathClaimRequest> scopedSpecification(DdfReportSearchDTO search, String username) {
        return DdfReportSpecification.getSpecification(search).and(
                CompanyScopeSpecification.companyCodeIn(companyAccessService.activeCompanyCodes(username),
                        "employee", "userPersonalDetails", "userCompanyDetails", "companyTypes", "code"));
    }

    private boolean canAccess(DeathClaimRequest claim, String username) {
        return claim.getEmployee() != null && claim.getEmployee().getUserPersonalDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes() != null
                && companyAccessService.canAccess(username, claim.getEmployee().getUserPersonalDetails()
                .getUserCompanyDetails().getCompanyTypes().getCode());
    }

    private Map<Long, PaymentAdviceDeathClaim> resolveAdviceByClaimId(List<DeathClaimRequest> claims) {
        if (claims == null || claims.isEmpty()) {
            return Map.of();
        }
        return paymentAdviceDeathClaimRepository.findAllByDeathClaimIn(claims).stream()
                .filter(adviceClaim -> adviceClaim.getDeathClaim() != null)
                .collect(Collectors.toMap(
                        adviceClaim -> adviceClaim.getDeathClaim().getId(),
                        adviceClaim -> adviceClaim,
                        (first, second) -> first
                ));
    }

    private DdfReportRowDTO mapRow(DeathClaimRequest claim, boolean generated, PaymentAdviceDeathClaim adviceClaim) {
        DdfReportRowDTO dto = new DdfReportRowDTO();
        dto.setId(claim.getId());
        dto.setRequestId(claim.getRequestId());

        if (claim.getEmployee() != null && claim.getEmployee().getUserPersonalDetails() != null) {
            UserPersonalDetails details = claim.getEmployee().getUserPersonalDetails();
            dto.setEpfNo(details.getEpfNo());
            dto.setEmployeeName(buildEmployeeName(details));

            UserCompanyDetails companyDetails = details.getUserCompanyDetails();
            if (companyDetails != null) {
                if (companyDetails.getCompanyTypes() != null) {
                    dto.setCompanyCode(companyDetails.getCompanyTypes().getCode());
                    dto.setCompanyDescription(companyDetails.getCompanyTypes().getDescription());
                }
            }
        }

        ClaimsDependents dependent = claim.getClaimsDependents();
        if (dependent != null && dependent.getRelationCategory() != null) {
            dto.setRelationCategory(dependent.getRelationCategory().name());
            dto.setRelationCategoryDescription(dependent.getRelationCategory().getDescription());
        }

        if (claim.getRequestStatus() != null) {
            dto.setStatus(claim.getRequestStatus().name());
            dto.setStatusDescription(claim.getRequestStatus().getDescription());
        }

        dto.setPaymentAdviceGenerated(generated);
        dto.setPaymentAdviceStatusDescription(generated ? "Generated" : "Not Generated");
        if (adviceClaim != null && adviceClaim.getPaymentAdvice() != null) {
            PaymentAdvice advice = adviceClaim.getPaymentAdvice();
            dto.setChequeNo(buildChequeNo(advice.getVoucherSequence()));
            dto.setChequeCreatedDate(advice.getCreatedDate());
        }
        dto.setApprovedAmount(claim.getApprovedAmount());
        dto.setDeathDate(claim.getDeathDate());
        dto.setCreatedDate(claim.getCreatedDate());
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

    private String buildEmployeeName(UserPersonalDetails details) {
        String firstName = details.getFirstName() != null ? details.getFirstName().trim() : "";
        String lastName = details.getLastName() != null ? details.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? details.getInitials() : fullName;
    }

    private String buildChequeNo(Integer sequence) {
        if (sequence == null) {
            return "";
        }
        final String prefix = "DDF";
        final int pad = 5;
        return prefix + String.format("%0" + pad + "d", sequence);
    }

    private byte[] buildExcel(List<DdfReportRowDTO> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DDF Report");

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
            sheet.setColumnWidth(1, 16 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 14 * 256);
            sheet.setColumnWidth(4, 18 * 256);
            sheet.setColumnWidth(5, 16 * 256);
            sheet.setColumnWidth(6, 18 * 256);
            sheet.setColumnWidth(7, 16 * 256);
            sheet.setColumnWidth(8, 16 * 256);
            sheet.setColumnWidth(9, 18 * 256);
            sheet.setColumnWidth(10, 14 * 256);
            sheet.setColumnWidth(11, 14 * 256);
            sheet.setColumnWidth(12, 14 * 256);
            sheet.setColumnWidth(13, 14 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("DDF Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 13));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Request ID", headerStyle);
            createStringCell(row, 2, "EPF No", headerStyle);
            createStringCell(row, 3, "Employee Name", headerStyle);
            createStringCell(row, 4, "Company", headerStyle);
            createStringCell(row, 5, "Relation", headerStyle);
            createStringCell(row, 6, "Status", headerStyle);
            createStringCell(row, 7, "Payment Advice", headerStyle);
            createStringCell(row, 8, "Cheque No", headerStyle);
            createStringCell(row, 9, "Cheque Created Date", headerStyle);
            createStringCell(row, 10, "Approved Amount", headerStyle);
            createStringCell(row, 11, "Death Date", headerStyle);
            createStringCell(row, 12, "Created Date", headerStyle);
            createStringCell(row, 13, "Claim ID", headerStyle);

            int lineNo = 1;
            for (DdfReportRowDTO rowDTO : rows) {
                row = sheet.createRow(rowIndex++);
                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, rowDTO.getRequestId(), dataStyle);
                createStringCell(row, 2, rowDTO.getEpfNo(), dataStyle);
                createStringCell(row, 3, rowDTO.getEmployeeName(), dataStyle);
                createStringCell(row, 4, buildDisplay(rowDTO.getCompanyCode(), rowDTO.getCompanyDescription()), dataStyle);
                createStringCell(row, 5, buildDisplay(rowDTO.getRelationCategory(), rowDTO.getRelationCategoryDescription()), dataStyle);
                createStringCell(row, 6, buildDisplay(rowDTO.getStatus(), rowDTO.getStatusDescription()), dataStyle);
                createStringCell(row, 7, rowDTO.getPaymentAdviceStatusDescription(), dataStyle);
                createStringCell(row, 8, rowDTO.getChequeNo(), dataStyle);
                createStringCell(row, 9, formatDate(rowDTO.getChequeCreatedDate()), dataStyle);
                createStringCell(row, 10, rowDTO.getApprovedAmount() != null ? rowDTO.getApprovedAmount().toString() : "", dataStyle);
                createStringCell(row, 11, formatDate(rowDTO.getDeathDate()), dataStyle);
                createStringCell(row, 12, formatDate(rowDTO.getCreatedDate()), dataStyle);
                createStringCell(row, 13, rowDTO.getId() != null ? rowDTO.getId().toString() : "", dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DDF report excel", e);
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

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }
}

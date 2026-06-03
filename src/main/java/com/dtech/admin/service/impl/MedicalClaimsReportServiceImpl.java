package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ClaimRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.ClaimsRequestResponseDTO;
import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.RelationCategory;
import com.dtech.admin.enums.RemarkCategory;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.enums.PaymentAttachmentStatus;
import com.dtech.admin.mapper.entityToDto.ClaimsApprovalEntityToDto;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.InsuranceClaimsDetails;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.model.PaymentAdviceAttachment;
import com.dtech.admin.model.PaymentAttachment;
import com.dtech.admin.model.PaymentAttachmentClaim;
import com.dtech.admin.model.Treatment;
import com.dtech.admin.model.TreatmentCategory;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.repository.PaymentAdviceAttachmentRepository;
import com.dtech.admin.repository.PaymentAttachmentClaimRepository;
import com.dtech.admin.repository.RemarkRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.repository.TreatmentCategoryRepository;
import com.dtech.admin.repository.TreatmentRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.MedicalClaimsReportService;
import com.dtech.admin.specifications.MedicalClaimsReportSpecification;
import com.dtech.admin.util.DateTimeUtil;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ApprovalRemarkUtil;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Log4j2
@RequiredArgsConstructor
public class MedicalClaimsReportServiceImpl implements MedicalClaimsReportService {

    private static final String PAGE_MEDICAL_CLAIMS_REPORT = WebPage.RPRT_MCRP.name();

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final PaymentAttachmentClaimRepository paymentAttachmentClaimRepository;

    @Autowired
    private final PaymentAdviceAttachmentRepository paymentAdviceAttachmentRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final TreatmentCategoryRepository treatmentCategoryRepository;

    @Autowired
    private final TreatmentRepository treatmentRepository;

    @Autowired
    private final RemarkRepository remarkRepository;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final ClaimsApprovalEntityToDto claimsApprovalEntityToDto;

    @Override
    @Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Medical claims report reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), PAGE_MEDICAL_CLAIMS_REPORT);

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Workflow.values())
                    .filter(wf -> Workflow.APPROVED.equals(wf) || Workflow.REJECTED.equals(wf))
                    .map(wf -> new SimpleBaseDTO(wf.name(), wf.getDescription()))
                    .toList();

            List<SimpleBaseDTO> treatmentCategory = treatmentCategoryRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> remarks = remarkRepository.findAllByRemarkCategoryAndStatus(RemarkCategory.INSURANCE, Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> treatment = treatmentRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getTreatmentCode(), val.getTreatmentDescription())).toList();

            List<SimpleBaseDTO> relationCategory = Arrays.stream(RelationCategory.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> paymentAdviceStatus = List.of(
                    new SimpleBaseDTO("GENERATED", "Generated"),
                    new SimpleBaseDTO("NOT_GENERATED", "Not Generated")
            );

            List<SimpleBaseDTO> companyTypes = companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                    .map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> staffCategories = staffCategoriesRepository.findAllByStatus(Status.ACTIVE).stream()
                    .map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            responseMap.put("treatmentCategory", treatmentCategory);
            responseMap.put("treatment", treatment);
            responseMap.put("relationCategory", relationCategory);
            responseMap.put("remarks", remarks);
            responseMap.put("paymentAdviceStatus", paymentAdviceStatus);
            responseMap.put("company", companyTypes);
            responseMap.put("staffCategories", staffCategories);

            auditLogService.log(PAGE_MEDICAL_CLAIMS_REPORT, WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.MEDICAL_CLAIMS_REPORT_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load medical claims report reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ClaimRequestSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Medical claims report filter list {}", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            ClaimRequestSearchDTO search = paginationRequest.getSearch();

            Page<InsuranceClaimsRequest> claimsPage = Objects.nonNull(search)
                    ? insuranceClaimsRequestRepository.findAll(MedicalClaimsReportSpecification.getSpecification(search), pageable)
                    : insuranceClaimsRequestRepository.findAll(MedicalClaimsReportSpecification.getSpecification(), pageable);

            long totalElements = Objects.nonNull(search)
                    ? insuranceClaimsRequestRepository.count(MedicalClaimsReportSpecification.getSpecification(search))
                    : insuranceClaimsRequestRepository.count(MedicalClaimsReportSpecification.getSpecification());

            List<InsuranceClaimsRequest> claims = claimsPage.getContent();
            Map<Long, String> paymentAdviceStatusMap = getPaymentAdviceStatusMap(claims);
            Map<Long, PaymentAdvice> adviceByClaimId = resolvePaymentAdviceByClaimId(claims);

            List<ClaimsRequestResponseDTO> responseDTOList = claimsPage.stream()
                    .map(claim -> {
                        ClaimsRequestResponseDTO dto = stripDocuments(claimsApprovalEntityToDto.mapClaimsApproval(claim, false));
                        String paymentAdviceStatus = paymentAdviceStatusMap.getOrDefault(claim.getId(), "NOT_GENERATED");
                        dto.setPaymentAdviceStatus(paymentAdviceStatus);
                        dto.setPaymentAdviceStatusDescription(resolvePaymentAdviceStatusDescription(paymentAdviceStatus));
                        populateMedicalReportFields(dto, claim, adviceByClaimId.get(claim.getId()));
                        return dto;
                    })
                    .toList();

            PagingResult<ClaimsRequestResponseDTO> result = new PagingResult<>(responseDTOList, responseDTOList.size(), totalElements);

            auditLogService.log(PAGE_MEDICAL_CLAIMS_REPORT, WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(responseDTOList), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) result,
                    messageSource.getMessage(ResponseMessageUtil.MEDICAL_CLAIMS_REPORT_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter medical claims report", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> view(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Medical claims report view {}", claimRequestDTO);
            return insuranceClaimsRequestRepository.findById(claimRequestDTO.getId()).map(claimsRequest -> {
                boolean settled = paymentAttachmentClaimRepository
                        .existsByInsuranceClaimsRequestAndPaymentAttachment_Status(claimsRequest, PaymentAttachmentStatus.FINALIZED);
                if (!settled) {
                    return ResponseEntity.ok().body(responseUtil.error(null, 1051,
                            messageSource.getMessage(ResponseMessageUtil.CLAIMS_DETAILS_NOT_FOUND,
                                    new Object[]{claimRequestDTO.getId()}, locale)));
                }

                ClaimsRequestResponseDTO claimsRequestResponseDTO = claimsApprovalEntityToDto.mapClaimsApproval(claimsRequest, true);
                String paymentAdviceStatus = getPaymentAdviceStatusMap(List.of(claimsRequest))
                        .getOrDefault(claimsRequest.getId(), "NOT_GENERATED");
                claimsRequestResponseDTO.setPaymentAdviceStatus(paymentAdviceStatus);
                claimsRequestResponseDTO.setPaymentAdviceStatusDescription(resolvePaymentAdviceStatusDescription(paymentAdviceStatus));
                populateMedicalReportFields(claimsRequestResponseDTO, claimsRequest,
                        resolvePaymentAdviceByClaimId(List.of(claimsRequest)).get(claimsRequest.getId()));
                auditLogService.log(PAGE_MEDICAL_CLAIMS_REPORT, WebTask.VIEW.name(),
                        AuditTask.VIEW_DATA.getDescription(), claimRequestDTO.getIp(),
                        claimRequestDTO.getUserAgent(), gson.toJson(claimsRequestResponseDTO), null, claimRequestDTO.getUsername());

                return ResponseEntity.ok().body(responseUtil.success((Object) claimsRequestResponseDTO,
                        messageSource.getMessage(ResponseMessageUtil.MEDICAL_CLAIMS_REPORT_VIEW_SUCCESS, null, locale)));
            }).orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 1051,
                    messageSource.getMessage(ResponseMessageUtil.CLAIMS_DETAILS_NOT_FOUND,
                            new Object[]{claimRequestDTO.getId()}, locale))));
        } catch (Exception e) {
            log.error("Failed to view medical claims report", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public ResponseEntity<byte[]> export(PaginationRequest<ClaimRequestSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Medical claims report export {}", paginationRequest);
            ClaimRequestSearchDTO search = paginationRequest.getSearch();

            List<InsuranceClaimsRequest> claims = Objects.nonNull(search)
                    ? insuranceClaimsRequestRepository.findAll(MedicalClaimsReportSpecification.getSpecification(search))
                    : insuranceClaimsRequestRepository.findAll(MedicalClaimsReportSpecification.getSpecification());

            byte[] excelBytes = buildExcel(claims);

            auditLogService.log(PAGE_MEDICAL_CLAIMS_REPORT, WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(search), null, paginationRequest.getUsername());

            String fileName = "medical-claims-report.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to export medical claims report", e);
            throw e;
        }
    }

    private byte[] buildExcel(List<InsuranceClaimsRequest> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Medical Claims Report");

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
            sheet.setColumnWidth(1, 18 * 256);
            sheet.setColumnWidth(2, 22 * 256);
            sheet.setColumnWidth(3, 20 * 256);
            sheet.setColumnWidth(4, 10 * 256);
            sheet.setColumnWidth(5, 20 * 256);
            sheet.setColumnWidth(6, 18 * 256);
            sheet.setColumnWidth(7, 20 * 256);
            sheet.setColumnWidth(8, 20 * 256);
            sheet.setColumnWidth(9, 16 * 256);
            sheet.setColumnWidth(10, 16 * 256);
            sheet.setColumnWidth(11, 14 * 256);
            sheet.setColumnWidth(12, 18 * 256);
            sheet.setColumnWidth(13, 18 * 256);
            sheet.setColumnWidth(14, 20 * 256);
            sheet.setColumnWidth(15, 20 * 256);
            sheet.setColumnWidth(16, 20 * 256);
            sheet.setColumnWidth(17, 20 * 256);
            sheet.setColumnWidth(18, 18 * 256);
            sheet.setColumnWidth(19, 22 * 256);
            sheet.setColumnWidth(20, 14 * 256);

            int rowIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellValue("Medical Claims Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 20));

            rowIndex++;
            row = sheet.createRow(rowIndex++);
            createStringCell(row, 0, "#", headerStyle);
            createStringCell(row, 1, "Request ID", headerStyle);
            createStringCell(row, 2, "Employee Name", headerStyle);
            createStringCell(row, 3, "Dependent Name", headerStyle);
            createStringCell(row, 4, "EPF", headerStyle);
            createStringCell(row, 5, "Company", headerStyle);
            createStringCell(row, 6, "Staff Category", headerStyle);
            createStringCell(row, 7, "Treatment", headerStyle);
            createStringCell(row, 8, "Treatment Category", headerStyle);
            createStringCell(row, 9, "Request Amount", headerStyle);
            createStringCell(row, 10, "Approved Amount", headerStyle);
            createStringCell(row, 11, "Remaining Balance", headerStyle);
            createStringCell(row, 12, "Claim Status", headerStyle);
            createStringCell(row, 13, "Advice No", headerStyle);
            createStringCell(row, 14, "Voucher No", headerStyle);
            createStringCell(row, 15, "Cheque Created Date", headerStyle);
            createStringCell(row, 16, "Final Remark", headerStyle);
            createStringCell(row, 17, "Final Approve Date", headerStyle);
            createStringCell(row, 18, "Rejection Date", headerStyle);
            createStringCell(row, 19, "Payment Advice Status", headerStyle);
            createStringCell(row, 20, "Created Date", headerStyle);

            int lineNo = 1;
            Map<Long, PaymentAdvice> adviceByClaimId = resolvePaymentAdviceByClaimId(rows);
            Map<Long, String> paymentAdviceStatusMap = getPaymentAdviceStatusMap(rows);
            for (InsuranceClaimsRequest rowDTO : rows) {
                row = sheet.createRow(rowIndex++);
                ClaimsDependents dependent = rowDTO.getClaimsDependents();
                UserPersonalDetails personal = rowDTO.getEmployee() != null
                        ? rowDTO.getEmployee().getUserPersonalDetails()
                        : null;
                UserCompanyDetails companyDetails = personal != null
                        ? personal.getUserCompanyDetails()
                        : null;

                String employeeName = buildEmployeeName(personal);
                String dependentName = buildDependentName(dependent);
                String epf = personal != null ? personal.getEpfNo() : "";
                String companyDisplay = buildDisplay(
                        companyDetails != null && companyDetails.getCompanyTypes() != null
                                ? companyDetails.getCompanyTypes().getCode()
                                : null,
                        companyDetails != null && companyDetails.getCompanyTypes() != null
                                ? companyDetails.getCompanyTypes().getDescription()
                                : null);
                String staffDisplay = buildDisplay(
                        companyDetails != null && companyDetails.getStaffCategories() != null
                                ? companyDetails.getStaffCategories().getCode()
                                : null,
                        companyDetails != null && companyDetails.getStaffCategories() != null
                                ? companyDetails.getStaffCategories().getDescription()
                                : null);

                InsuranceClaimsDetails details = rowDTO.getInsuranceClaimsDetails();
                Treatment treatment = details != null ? details.getTreatment() : null;
                TreatmentCategory treatmentCategory = details != null ? details.getTreatmentCategory() : null;

                String treatmentDisplay = treatment != null
                        ? buildDisplay(treatment.getTreatmentCode(), treatment.getTreatmentDescription())
                        : "";

                String treatmentCategoryDisplay = treatmentCategory != null
                        ? buildDisplay(treatmentCategory.getCode(), treatmentCategory.getDescription())
                        : "";

                PaymentAdvice advice = rowDTO.getId() != null ? adviceByClaimId.get(rowDTO.getId()) : null;
                BigDecimal remainingBalance = calculateRemainingBalance(rowDTO.getRequestAmount(), rowDTO.getApprovedAmount());
                String finalApproveDate = formatDate(resolveFinalApproveDate(rowDTO));
                String rejectionDate = formatDate(resolveRejectionDate(rowDTO));

                createStringCell(row, 0, String.valueOf(lineNo++), dataStyle);
                createStringCell(row, 1, safeString(rowDTO.getRequestId()), dataStyle);
                createStringCell(row, 2, employeeName, dataStyle);
                createStringCell(row, 3, dependentName, dataStyle);
                createStringCell(row, 4, safeString(epf), dataStyle);
                createStringCell(row, 5, companyDisplay, dataStyle);
                createStringCell(row, 6, staffDisplay, dataStyle);
                createStringCell(row, 7, treatmentDisplay, dataStyle);
                createStringCell(row, 8, treatmentCategoryDisplay, dataStyle);
                createStringCell(row, 9, toAmountString(rowDTO.getRequestAmount()), dataStyle);
                createStringCell(row, 10, toAmountString(rowDTO.getApprovedAmount()), dataStyle);
                createStringCell(row, 11, toAmountString(remainingBalance), dataStyle);
                createStringCell(row, 12, rowDTO.getRequestStatus() != null ? rowDTO.getRequestStatus().getDescription() : "", dataStyle);
                createStringCell(row, 13, resolveAdviceNo(advice), dataStyle);
                createStringCell(row, 14, resolveChequeNo(advice), dataStyle);
                createStringCell(row, 15, formatDate(advice != null ? advice.getCreatedDate() : null), dataStyle);
                createStringCell(row, 16, resolveFinalRemark(rowDTO), dataStyle);
                createStringCell(row, 17, finalApproveDate, dataStyle);
                createStringCell(row, 18, rejectionDate, dataStyle);
                String paymentAdviceStatus = rowDTO.getId() != null
                        ? paymentAdviceStatusMap.getOrDefault(rowDTO.getId(), "NOT_GENERATED")
                        : "NOT_GENERATED";
                createStringCell(row, 19, resolvePaymentAdviceStatusDescription(paymentAdviceStatus), dataStyle);
                createStringCell(row, 20, formatDate(rowDTO.getCreatedDate()), dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate medical claims report excel", e);
        }
    }

    private ClaimsRequestResponseDTO stripDocuments(ClaimsRequestResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        if (dto.getInsuranceClaimsDetails() != null) {
            dto.getInsuranceClaimsDetails().setDocuments(null);
        }
        if (dto.getEmployee() != null && dto.getEmployee().getUserPersonalDetails() != null) {
            dto.getEmployee().getUserPersonalDetails().setBirthImg(null);
            dto.getEmployee().getUserPersonalDetails().setMaritalStatusDocument(null);
            if (dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails() != null) {
                dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails().setPromoDoc(null);
            }
        }
        return dto;
    }

    private void populateMedicalReportFields(ClaimsRequestResponseDTO dto,
                                             InsuranceClaimsRequest claim,
                                             PaymentAdvice advice) {
        if (dto == null || claim == null) {
            return;
        }
        dto.setAdviceNo(resolveAdviceNo(advice));
        dto.setFinalRemark(resolveFinalRemark(claim));
        dto.setFinalApproveDate(resolveFinalApproveDate(claim));
        dto.setRejectionDate(resolveRejectionDate(claim));
    }

    private List<InsuranceClaimsRequest> filterByFinalDecisionDate(List<InsuranceClaimsRequest> claims,
                                                                   ClaimRequestSearchDTO search) {
        if (claims == null || search == null || (!hasText(search.getFromDate()) && !hasText(search.getToDate()))) {
            return claims;
        }

        Date fromDate = parseBoundaryDate(search.getFromDate(), true);
        Date toDate = parseBoundaryDate(search.getToDate(), false);

        return claims.stream()
                .filter(claim -> {
                    Date decisionDate = resolveReportDecisionDate(claim);
                    if (decisionDate == null) {
                        return false;
                    }
                    if (fromDate != null && decisionDate.before(fromDate)) {
                        return false;
                    }
                    return toDate == null || !decisionDate.after(toDate);
                })
                .toList();
    }

    private Date resolveReportDecisionDate(InsuranceClaimsRequest claim) {
        if (claim == null) {
            return null;
        }
        if (Workflow.APPROVED.equals(claim.getRequestStatus())) {
            return resolveFinalApproveDate(claim);
        }
        if (Workflow.REJECTED.equals(claim.getRequestStatus())) {
            return resolveRejectionDate(claim);
        }
        return null;
    }

    private Date parseBoundaryDate(String value, boolean startOfDay) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return startOfDay
                    ? DateTimeUtil.getStartOfDay(normalizeDate(value))
                    : DateTimeUtil.getEndOfDay(normalizeDate(value));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date range", e);
        }
    }

    private String resolveFinalRemark(InsuranceClaimsRequest claim) {
        String remark = ApprovalRemarkUtil.resolveLevelTwoOrThreeRemark(claim);
        return remark != null ? remark : "";
    }

    private java.util.Date resolveFinalApproveDate(InsuranceClaimsRequest claim) {
        if (claim == null || !Workflow.APPROVED.equals(claim.getRequestStatus())) {
            return null;
        }
        return resolveFinalDecisionDate(claim, Workflow.APPROVED);
    }

    private java.util.Date resolveRejectionDate(InsuranceClaimsRequest claim) {
        if (claim == null || !Workflow.REJECTED.equals(claim.getRequestStatus())) {
            return null;
        }
        return resolveFinalDecisionDate(claim, Workflow.REJECTED);
    }

    private java.util.Date resolveFinalDecisionDate(InsuranceClaimsRequest claim, Workflow status) {
        if (claim == null || claim.getApprovalWorkFlows() == null || claim.getApprovalWorkFlows().isEmpty()) {
            return null;
        }
        return claim.getApprovalWorkFlows().stream()
                .filter(Objects::nonNull)
                .filter(flow -> flow.getStatus() == status)
                .filter(flow -> flow.getApprovalLevel() == ApprovalLevel.LEVEL02
                        || flow.getApprovalLevel() == ApprovalLevel.LEVEL03)
                .map(ApprovalWorkFlow::getApprovedDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private Map<Long, String> getPaymentAdviceStatusMap(List<InsuranceClaimsRequest> claims) {
        Map<Long, String> statusMap = new HashMap<>();
        if (claims == null || claims.isEmpty()) {
            return statusMap;
        }

        List<PaymentAttachmentClaim> attachmentClaims = paymentAttachmentClaimRepository.findAllByInsuranceClaimsRequestIn(claims);
        if (attachmentClaims == null || attachmentClaims.isEmpty()) {
            for (InsuranceClaimsRequest claim : claims) {
                if (claim != null && claim.getId() != null) {
                    statusMap.put(claim.getId(), "NOT_GENERATED");
                }
            }
            return statusMap;
        }

        Map<Long, List<Long>> claimAttachmentIds = new HashMap<>();
        Map<Long, PaymentAttachment> attachmentsById = new HashMap<>();

        for (PaymentAttachmentClaim attachmentClaim : attachmentClaims) {
            if (attachmentClaim == null) {
                continue;
            }
            InsuranceClaimsRequest claim = attachmentClaim.getInsuranceClaimsRequest();
            PaymentAttachment attachment = attachmentClaim.getPaymentAttachment();
            if (claim == null || claim.getId() == null || attachment == null || attachment.getId() == null) {
                continue;
            }
            claimAttachmentIds.computeIfAbsent(claim.getId(), key -> new ArrayList<>()).add(attachment.getId());
            attachmentsById.putIfAbsent(attachment.getId(), attachment);
        }

        List<PaymentAdviceAttachment> adviceAttachments = attachmentsById.isEmpty()
                ? List.of()
                : paymentAdviceAttachmentRepository.findAllByPaymentAttachmentIn(new ArrayList<>(attachmentsById.values()));

        Set<Long> attachmentsWithAdvice = new HashSet<>();
        for (PaymentAdviceAttachment adviceAttachment : adviceAttachments) {
            PaymentAttachment attachment = adviceAttachment.getPaymentAttachment();
            if (attachment != null && attachment.getId() != null) {
                attachmentsWithAdvice.add(attachment.getId());
            }
        }

        for (InsuranceClaimsRequest claim : claims) {
            if (claim == null || claim.getId() == null) {
                continue;
            }
            List<Long> attachmentIds = claimAttachmentIds.get(claim.getId());
            String status = "NOT_GENERATED";
            if (attachmentIds != null) {
                for (Long attachmentId : attachmentIds) {
                    if (attachmentsWithAdvice.contains(attachmentId)) {
                        status = "GENERATED";
                        break;
                    }
                }
            }
            statusMap.put(claim.getId(), status);
        }

        return statusMap;
    }

    private String resolvePaymentAdviceStatusDescription(String status) {
        if ("GENERATED".equalsIgnoreCase(status)) {
            return "Generated";
        }
        if ("NOT_GENERATED".equalsIgnoreCase(status)) {
            return "Not Generated";
        }
        return "";
    }

    private Map<Long, PaymentAdvice> resolvePaymentAdviceByClaimId(List<InsuranceClaimsRequest> claims) {
        if (claims == null || claims.isEmpty()) {
            return Map.of();
        }

        List<PaymentAttachmentClaim> attachmentClaims = paymentAttachmentClaimRepository
                .findAllByInsuranceClaimsRequestIn(claims);
        if (attachmentClaims == null || attachmentClaims.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> claimAttachmentIds = new HashMap<>();
        Map<Long, PaymentAttachment> attachmentsById = new HashMap<>();

        for (PaymentAttachmentClaim attachmentClaim : attachmentClaims) {
            if (attachmentClaim == null) {
                continue;
            }
            InsuranceClaimsRequest claim = attachmentClaim.getInsuranceClaimsRequest();
            PaymentAttachment attachment = attachmentClaim.getPaymentAttachment();
            if (claim == null || claim.getId() == null || attachment == null || attachment.getId() == null) {
                continue;
            }
            claimAttachmentIds.computeIfAbsent(claim.getId(), key -> new ArrayList<>()).add(attachment.getId());
            attachmentsById.putIfAbsent(attachment.getId(), attachment);
        }

        List<PaymentAdviceAttachment> adviceAttachments = attachmentsById.isEmpty()
                ? List.of()
                : paymentAdviceAttachmentRepository.findAllByPaymentAttachmentIn(new ArrayList<>(attachmentsById.values()));

        Map<Long, PaymentAdvice> adviceByAttachmentId = new HashMap<>();
        for (PaymentAdviceAttachment adviceAttachment : adviceAttachments) {
            PaymentAttachment attachment = adviceAttachment.getPaymentAttachment();
            PaymentAdvice advice = adviceAttachment.getPaymentAdvice();
            if (attachment != null && attachment.getId() != null && advice != null) {
                adviceByAttachmentId.putIfAbsent(attachment.getId(), advice);
            }
        }

        Map<Long, PaymentAdvice> adviceByClaimId = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : claimAttachmentIds.entrySet()) {
            Long claimId = entry.getKey();
            for (Long attachmentId : entry.getValue()) {
                PaymentAdvice advice = adviceByAttachmentId.get(attachmentId);
                if (advice != null) {
                    adviceByClaimId.putIfAbsent(claimId, advice);
                    break;
                }
            }
        }
        return adviceByClaimId;
    }

    private String resolveChequeNo(PaymentAdvice advice) {
        if (advice == null) {
            return "";
        }
        if (advice.getVoucherNo() != null && !advice.getVoucherNo().isBlank()) {
            return advice.getVoucherNo();
        }
        if (advice.getVoucherSequence() != null) {
            return "HC/" + String.format("%07d", advice.getVoucherSequence());
        }
        return "";
    }

    private String resolveAdviceNo(PaymentAdvice advice) {
        if (advice == null || advice.getAdviceNo() == null) {
            return "";
        }
        return advice.getAdviceNo();
    }

    private String buildDependentName(ClaimsDependents dependent) {
        if (dependent == null) {
            return "";
        }
        String firstName = dependent.getFirstName() != null ? dependent.getFirstName().trim() : "";
        String lastName = dependent.getLastName() != null ? dependent.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        return dependent.getInitials() != null ? dependent.getInitials() : "";
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

    private String buildEmployeeName(UserPersonalDetails personal) {
        if (personal == null) {
            return "";
        }
        String first = personal.getFirstName() != null ? personal.getFirstName().trim() : "";
        String last = personal.getLastName() != null ? personal.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return !full.isEmpty() ? full : personal.getInitials();
    }

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    private String normalizeDate(String value) {
        return value.contains("-") ? value.replace("-", "/") : value;
    }

    private String toAmountString(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "0";
    }

    private BigDecimal calculateRemainingBalance(BigDecimal requestAmount, BigDecimal approvedAmount) {
        BigDecimal request = requestAmount != null ? requestAmount : BigDecimal.ZERO;
        BigDecimal approved = approvedAmount != null ? approvedAmount : BigDecimal.ZERO;
        return request.subtract(approved);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }
}

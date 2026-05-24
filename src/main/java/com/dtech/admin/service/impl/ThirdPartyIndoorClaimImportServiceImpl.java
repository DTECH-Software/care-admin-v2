package com.dtech.admin.service.impl;

import com.dtech.admin.dto.ClaimRequestIdGen;
import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.ThirdPartyIndoorClaimBatchRequestDTO;
import com.dtech.admin.dto.request.ThirdPartyIndoorClaimFileRequestDTO;
import com.dtech.admin.dto.response.*;
import com.dtech.admin.dto.search.ThirdPartyIndoorClaimBatchSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.ThirdPartyIndoorClaimImportService;
import com.dtech.admin.specifications.ThirdPartyIndoorClaimBatchSpecification;
import com.dtech.admin.util.*;
import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class ThirdPartyIndoorClaimImportServiceImpl implements ThirdPartyIndoorClaimImportService {

    private static final String PAGE_CODE = com.dtech.admin.enums.WebPage.TPIC.name();
    private static final String FIXED_TREATMENT_CODE = TreatmentType.INDOOR.name();
    private static final String FIXED_TREATMENT_CATEGORY_CODE = com.dtech.admin.enums.TreatmentCategory.OTHER.name();
    private static final String IMPORTED_CLAIM_DISEASE = "Third Party Indoor Claim";
    private static final String TEMPLATE_FILE_NAME = "third-party-indoor-claims-template.xlsx";
    private static final List<String> TEMPLATE_HEADERS = List.of(
            "thirdPartyReferenceNo",
            "companyCode",
            "epfNo",
            "policyNo",
            "Policy Period From",
            "Policy Period To",
            "intimatedDate",
            "paidDate",
            "nonPayableAmount",
            "nonPayableItem",
            "claimAmount",
            "Paid Amount",
            "remark"
    );
    private static final List<String> REQUIRED_HEADERS = List.of(
            "thirdPartyReferenceNo",
            "companyCode",
            "epfNo",
            "policyNo",
            "policyPeriodFrom",
            "policyPeriodTo",
            "intimatedDate",
            "paidDate",
            "paidAmount"
    );
    private static final Map<String, List<String>> HEADER_ALIASES = Map.of(
            "policyPeriodFrom", List.of("policyPeriodFrom", "fromDate"),
            "policyPeriodTo", List.of("policyPeriodTo", "toDate"),
            "nonPayableAmount", List.of("nonPayableAmount", "nonPayable"),
            "claimAmount", List.of("claimAmount"),
            "paidAmount", List.of("paidAmount", "approvedAmount")
    );
    private static final List<Facility> INSURANCE_FACILITIES = List.of(Facility.INSURANCE, Facility.BOTH);
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

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
    private final ThirdPartyIndoorClaimImportBatchRepository batchRepository;

    @Autowired
    private final ThirdPartyIndoorClaimImportRowRepository rowRepository;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final TreatmentRepository treatmentRepository;

    @Autowired
    private final TreatmentCategoryRepository treatmentCategoryRepository;

    @Autowired
    private final InsuranceStaffCategoryPeriodRepository insuranceStaffCategoryPeriodRepository;

    @Autowired
    private final InsuranceDetailsLimitRepository insuranceDetailsLimitRepository;

    @Autowired
    private final InsuranceQuarterRepository insuranceQuarterRepository;

    @Autowired
    private final EntityManager entityManager;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("privileges", commonPrivilegeGetter.getPrivileges(channelRequestDTO.getUsername(), PAGE_CODE));
            responseMap.put("batchStatuses", Arrays.stream(ThirdPartyIndoorClaimBatchStatus.values())
                    .map(status -> new SimpleBaseDTO(status.name(), status.getDescription()))
                    .toList());
            responseMap.put("fixedTreatment", new SimpleBaseDTO(FIXED_TREATMENT_CODE, TreatmentType.INDOOR.getDescription()));
            responseMap.put("fixedTreatmentCategory", new SimpleBaseDTO(FIXED_TREATMENT_CATEGORY_CODE, "Other"));
            responseMap.put("templateColumns", TEMPLATE_HEADERS.stream()
                    .map(header -> new SimpleBaseDTO(header, header))
                    .toList());
            responseMap.put("rules", List.of(
                    "Only active non-NS employees are allowed.",
                    "Only indoor claims are allowed.",
                    "companyCode and epfNo are used to identify the employee.",
                    "Policy Period From and Policy Period To are used to map the insurance period.",
                    "Paid Amount is entered manually.",
                    "claimAmount can be entered manually.",
                    "If claimAmount is blank or 0, it is calculated by the system as nonPayableAmount + paidAmount.",
                    "Blank nonPayableAmount is considered as 0.",
                    "nonPayableItem is required when nonPayableAmount is greater than zero."
            ));

            auditLogService.log(PAGE_CODE, com.dtech.admin.enums.WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(),
                    channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.THIRD_PARTY_INDOOR_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load third party indoor claim import reference data", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> downloadTemplate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("third-party-indoor-claims");
            createTemplateHeader(workbook, sheet);

            workbook.write(out);
            auditLogService.log(PAGE_CODE, com.dtech.admin.enums.WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                    channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), TEMPLATE_FILE_NAME, null, channelRequestDTO.getUsername());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + TEMPLATE_FILE_NAME + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate third party indoor claim template", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> validate(ThirdPartyIndoorClaimFileRequestDTO requestDTO, Locale locale) {
        try {
            ValidationResult validationResult = validateFile(requestDTO);
            auditLogService.log(PAGE_CODE, com.dtech.admin.enums.WebTask.FILE_UPLOAD.name(), AuditTask.VIEW_DATA.getDescription(),
                    requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(validationResult.toResponse()), null, requestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(validationResult.toResponse(),
                    messageSource.getMessage(ResponseMessageUtil.THIRD_PARTY_INDOOR_VALIDATE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to validate third party indoor claim import file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> importClaims(ThirdPartyIndoorClaimFileRequestDTO requestDTO, Locale locale) {
        try {
            ValidationResult validationResult = validateFile(requestDTO);
            if (validationResult.invalidRows() > 0) {
                return ResponseEntity.ok().body(responseUtil.error(
                        buildImportValidationErrors(validationResult),
                        1001,
                        messageSource.getMessage(ResponseMessageUtil.THIRD_PARTY_INDOOR_IMPORT_VALIDATION_FAILED, null, locale)
                ));
            }

            ThirdPartyIndoorClaimImportBatch batch = new ThirdPartyIndoorClaimImportBatch();
            batch.setBatchNo("PENDING");
            batch.setFileName(requestDTO.getFileName());
            batch.setFileType(requestDTO.getFileType());
            batch.setTotalRows(validationResult.totalRows());
            batch.setValidRows(validationResult.validRows());
            batch.setInvalidRows(validationResult.invalidRows());
            batch.setDuplicateRows(validationResult.duplicateRows());
            batch.setImportedRows(0);
            batch.setStatus(ThirdPartyIndoorClaimBatchStatus.FAILED);
            batch = batchRepository.saveAndFlush(batch);
            batch.setBatchNo(generateBatchNo(batch.getId()));

            int importedRows = 0;
            int failedRows = 0;

            for (RowValidation rowValidation : validationResult.rows()) {
                ThirdPartyIndoorClaimImportRow row = mapBatchRow(rowValidation, batch);

                if (rowValidation.status() == ValidationStatus.DUPLICATE) {
                    row.setStatus(ThirdPartyIndoorClaimRowStatus.DUPLICATE);
                    row.setErrorMessage("Claim already imported for reference " + rowValidation.externalReferenceNo());
                    batch.getRows().add(row);
                    continue;
                }

                try {
                    InsuranceClaimsRequest claim = createImportedClaim(rowValidation, requestDTO.getUsername());
                    row.setStatus(ThirdPartyIndoorClaimRowStatus.IMPORTED);
                    row.setInsuranceClaim(claim);
                    importedRows++;
                } catch (Exception ex) {
                    log.error("Failed to import row {}", rowValidation.rowNo(), ex);
                    row.setStatus(ThirdPartyIndoorClaimRowStatus.FAILED);
                    row.setErrorMessage(ex.getMessage());
                    failedRows++;
                }

                batch.getRows().add(row);
            }

            batch.setImportedRows(importedRows);
            batch.setInvalidRows(batch.getInvalidRows() + failedRows);
            if (importedRows == 0) {
                batch.setStatus(ThirdPartyIndoorClaimBatchStatus.FAILED);
            } else if (batch.getDuplicateRows() > 0 || failedRows > 0) {
                batch.setStatus(ThirdPartyIndoorClaimBatchStatus.PARTIAL_IMPORTED);
            } else {
                batch.setStatus(ThirdPartyIndoorClaimBatchStatus.IMPORTED);
            }

            batch = batchRepository.saveAndFlush(batch);

            ThirdPartyIndoorClaimBatchResponseDTO responseDTO = mapBatchResponse(batch, rowRepository.findAllByBatchOrderByRowNoAsc(batch));

            auditLogService.log(PAGE_CODE, com.dtech.admin.enums.WebTask.ADD.name(), AuditTask.ADD_DATA.getDescription(),
                    requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(responseDTO), null, requestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.THIRD_PARTY_INDOOR_IMPORT_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to import third party indoor claims", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ThirdPartyIndoorClaimBatchSearchDTO> paginationRequest, Locale locale) {
        try {
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            ThirdPartyIndoorClaimBatchSearchDTO filter = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(ThirdPartyIndoorClaimBatchSearchDTO::new);

            Page<ThirdPartyIndoorClaimImportBatch> page = batchRepository.findAll(
                    ThirdPartyIndoorClaimBatchSpecification.getSpecification(filter), pageable);
            long total = batchRepository.count(ThirdPartyIndoorClaimBatchSpecification.getSpecification(filter));

            List<ThirdPartyIndoorClaimBatchListResponseDTO> response = page.stream()
                    .map(this::mapBatchListResponse)
                    .toList();

            auditLogService.log(PAGE_CODE, com.dtech.admin.enums.WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(),
                    paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(response), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(
                    new PagingResult<>(response, response.size(), total),
                    messageSource.getMessage(ResponseMessageUtil.THIRD_PARTY_INDOOR_FILTER_SUCCESS, null, locale)
            ));
        } catch (Exception e) {
            log.error("Failed to filter third party indoor claim import batches", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(ThirdPartyIndoorClaimBatchRequestDTO requestDTO, Locale locale) {
        try {
            return batchRepository.findById(requestDTO.getId()).map(batch -> {
                List<ThirdPartyIndoorClaimImportRow> rows = rowRepository.findAllByBatchOrderByRowNoAsc(batch);
                ThirdPartyIndoorClaimBatchResponseDTO responseDTO = mapBatchResponse(batch, rows);

                auditLogService.log(PAGE_CODE, com.dtech.admin.enums.WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                        requestDTO.getIp(), requestDTO.getUserAgent(), gson.toJson(responseDTO), null, requestDTO.getUsername());

                return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                        messageSource.getMessage(ResponseMessageUtil.THIRD_PARTY_INDOOR_VIEW_SUCCESS, null, locale)));
            }).orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 1051,
                    messageSource.getMessage(ResponseMessageUtil.THIRD_PARTY_INDOOR_BATCH_NOT_FOUND,
                            new Object[]{requestDTO.getId()}, locale))));
        } catch (Exception e) {
            log.error("Failed to view third party indoor claim import batch {}", requestDTO.getId(), e);
            throw new RuntimeException(e);
        }
    }

    private ValidationResult validateFile(ThirdPartyIndoorClaimFileRequestDTO requestDTO) throws Exception {
        byte[] fileBytes = decodeFile(requestDTO.getFile());
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel sheet is empty");
            }

            Map<String, Integer> headerIndex = resolveHeaderIndex(sheet);
            Treatment treatment = treatmentRepository.findByTreatmentCodeAndStatus(FIXED_TREATMENT_CODE, Status.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Indoor treatment is not configured"));
            com.dtech.admin.model.TreatmentCategory treatmentCategory = treatmentCategoryRepository
                    .findByCodeAndStatus(FIXED_TREATMENT_CATEGORY_CODE, Status.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Other treatment category is not configured"));

            List<RowValidation> rows = new ArrayList<>();
            Set<String> fileReferences = new HashSet<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row)) {
                    continue;
                }
                rows.add(validateRow(row, headerIndex, treatment, treatmentCategory, fileReferences));
            }

            return new ValidationResult(requestDTO.getFileName(), rows);
        }
    }

    private RowValidation validateRow(Row row,
                                      Map<String, Integer> headerIndex,
                                      Treatment treatment,
                                      com.dtech.admin.model.TreatmentCategory treatmentCategory,
                                      Set<String> fileReferences) {
        DataFormatter formatter = new DataFormatter();
        int rowNo = row.getRowNum() + 1;
        List<String> errors = new ArrayList<>();

        String externalReferenceNo = getString(row, headerIndex, "thirdPartyReferenceNo", formatter);
        String companyCode = getString(row, headerIndex, "companyCode", formatter);
        String epfNo = getString(row, headerIndex, "epfNo", formatter);
        String policyNo = getString(row, headerIndex, "policyNo", formatter);
        String nonPayableItem = getString(row, headerIndex, "nonPayableItem", formatter);
        String remark = getString(row, headerIndex, "remark", formatter);

        Date fromDate = getDate(row, headerIndex, "policyPeriodFrom", formatter, errors);
        Date toDate = getDate(row, headerIndex, "policyPeriodTo", formatter, errors);
        Date intimatedDate = getDate(row, headerIndex, "intimatedDate", formatter, errors);
        Date paidDate = getDate(row, headerIndex, "paidDate", formatter, errors);
        BigDecimal approvedAmount = getBigDecimal(row, headerIndex, "paidAmount", formatter, errors);
        BigDecimal nonPayableAmount = getOptionalBigDecimal(row, headerIndex, "nonPayableAmount", formatter, errors);
        if (nonPayableAmount == null) {
            nonPayableAmount = BigDecimal.ZERO;
        }
        BigDecimal uploadedClaimAmount = getOptionalBigDecimal(row, headerIndex, "claimAmount", formatter, errors);
        BigDecimal claimAmount = resolveClaimAmount(uploadedClaimAmount, nonPayableAmount, approvedAmount);
        Integer policyYear = null;

        if (!hasText(externalReferenceNo)) {
            errors.add("thirdPartyReferenceNo is required");
        }

        String normalizedReference = normalizeReference(externalReferenceNo);
        if (hasText(normalizedReference) && !fileReferences.add(normalizedReference)) {
            errors.add("Duplicate thirdPartyReferenceNo found in the uploaded file");
        }

        if (!hasText(companyCode)) {
            errors.add("companyCode is required");
        }

        if (!hasText(epfNo)) {
            errors.add("epfNo is required");
        }

        if (!hasText(policyNo)) {
            errors.add("policyNo is required");
        }

        if (fromDate != null && toDate != null && fromDate.after(toDate)) {
            errors.add("policyPeriodFrom cannot be after policyPeriodTo");
        }

        if (intimatedDate != null && paidDate != null && intimatedDate.after(paidDate)) {
            errors.add("intimatedDate cannot be after paidDate");
        }

        if (nonPayableAmount.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("nonPayableAmount cannot be negative");
        }

        if (claimAmount != null && claimAmount.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("claimAmount cannot be negative");
        }

        if (approvedAmount != null && approvedAmount.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("paidAmount cannot be negative");
        }

        if (nonPayableAmount.compareTo(BigDecimal.ZERO) > 0
                && !hasText(nonPayableItem)) {
            errors.add("nonPayableItem is required when nonPayableAmount is greater than zero");
        }

        ApplicationUser employee = null;
        String employeeName = null;
        InsuranceStaffCategoryPeriod insurancePeriod = null;
        InsuranceDetailsLimit insuranceDetailsLimit = null;
        InsuranceQuarter insuranceQuarter = null;

        if (errors.isEmpty()) {
            employee = applicationUserRepository
                    .findByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_UserCompanyDetails_CompanyTypes_CodeAndUserPersonalDetails_UserStatus(
                            epfNo.trim(), companyCode.trim(), Status.ACTIVE)
                    .orElse(null);

            if (employee == null) {
                errors.add("Employee not found for the given companyCode and epfNo");
            } else {
                employeeName = buildEmployeeName(employee);

                UserCompanyDetails companyDetails = employee.getUserPersonalDetails().getUserCompanyDetails();
                if (companyDetails == null || companyDetails.getStaffCategories() == null) {
                    errors.add("Employee staff category is missing");
                } else if ("NS".equalsIgnoreCase(companyDetails.getStaffCategories().getCode())) {
                    errors.add("NS staff category is not allowed for this import");
                }

                if (companyDetails == null || companyDetails.getFacility() == null || !INSURANCE_FACILITIES.contains(companyDetails.getFacility())) {
                    errors.add("Employee is not eligible for insurance claims");
                }

                if (companyDetails == null || companyDetails.getInsurancePolicy() == null) {
                    errors.add("Employee insurance policy is missing");
                } else if (errors.isEmpty()) {
                    insurancePeriod = insuranceStaffCategoryPeriodRepository
                            .findByDateWithinRange(fromDate, companyDetails.getStaffCategories().getCode())
                            .orElse(null);
                    if (insurancePeriod == null || !Status.ACTIVE.equals(insurancePeriod.getStatus())) {
                        errors.add("Insurance period not found for policy period dates");
                    } else if (!isDateWithinPeriod(toDate, insurancePeriod)) {
                        errors.add("Policy period dates do not fall within one insurance period");
                    } else {
                        policyYear = insurancePeriod.getFromDate() != null
                                ? DateTimeUtil.getYear(insurancePeriod.getFromDate())
                                : DateTimeUtil.getYear(fromDate);
                    }

                    if (errors.isEmpty()) {
                        insuranceDetailsLimit = insuranceDetailsLimitRepository.findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment(
                                companyDetails.getInsurancePolicy(), Status.ACTIVE, insurancePeriod, treatment).orElse(null);
                        if (insuranceDetailsLimit == null) {
                            errors.add("Indoor insurance limit is not configured for the employee policy");
                        } else if (Boolean.TRUE.equals(insuranceDetailsLimit.getIsQuarter())) {
                            insuranceQuarter = insuranceQuarterRepository.findByDateWithinRangeAndCodeWithLimit(
                                            insuranceDetailsLimit, FIXED_TREATMENT_CATEGORY_CODE, toDate)
                                    .stream()
                                    .findFirst()
                                    .orElse(null);
                        }
                    }
                }
            }
        }

        if (errors.isEmpty() && rowRepository.existsByExternalReferenceNoIgnoreCaseAndInsuranceClaimIsNotNull(externalReferenceNo.trim())) {
            return new RowValidation(rowNo, externalReferenceNo.trim(), companyCode, epfNo, employeeName,
                    policyYear, policyNo, fromDate, toDate, intimatedDate, paidDate,
                    nonPayableAmount, nonPayableItem, claimAmount, approvedAmount, remark, ValidationStatus.DUPLICATE, List.of(),
                    employee, insurancePeriod, insuranceDetailsLimit, insuranceQuarter, treatment, treatmentCategory);
        }

        ValidationStatus status = errors.isEmpty() ? ValidationStatus.VALID : ValidationStatus.FAILED;
        return new RowValidation(rowNo, trimToNull(externalReferenceNo), trimToNull(companyCode), trimToNull(epfNo),
                employeeName, policyYear, trimToNull(policyNo),
                fromDate, toDate, intimatedDate, paidDate,
                nonPayableAmount, trimToNull(nonPayableItem), claimAmount, approvedAmount,
                trimToNull(remark), status, errors, employee, insurancePeriod, insuranceDetailsLimit,
                insuranceQuarter, treatment, treatmentCategory);
    }

    private BigDecimal resolveClaimAmount(BigDecimal uploadedClaimAmount, BigDecimal nonPayableAmount, BigDecimal paidAmount) {
        if (uploadedClaimAmount != null && uploadedClaimAmount.compareTo(BigDecimal.ZERO) != 0) {
            return uploadedClaimAmount;
        }
        if (paidAmount == null) {
            return null;
        }
        BigDecimal nonPayable = nonPayableAmount != null ? nonPayableAmount : BigDecimal.ZERO;
        return nonPayable.add(paidAmount);
    }

    private InsuranceClaimsRequest createImportedClaim(RowValidation rowValidation, String username) {
        ClaimRequestIdGen claimRequestIdGen = ClaimRequestIdGen.builder()
                .year(String.valueOf(rowValidation.policyYear()))
                .company(rowValidation.employee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode())
                .staffCategory(rowValidation.employee().getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode())
                .build();

        RequestIdGenUtil requestIdGenUtil = new RequestIdGenUtil(true);
        String requestId = (String) requestIdGenUtil.generate(entityManager.unwrap(SharedSessionContractImplementor.class), claimRequestIdGen);

        ApprovalWorkFlow approvalWorkFlow = new ApprovalWorkFlow();
        approvalWorkFlow.setApprovalLevel(ApprovalLevel.LEVEL03);
        approvalWorkFlow.setApprovedDate(DateTimeUtil.getCurrentDateTime());
        approvalWorkFlow.setApprovedUser(username);
        approvalWorkFlow.setStatus(Workflow.APPROVED);
        approvalWorkFlow.setApprovedAmount(rowValidation.approvedAmount());
        approvalWorkFlow.setPolicy(rowValidation.insurancePeriod());

        InsuranceClaimsDetails claimDetails = new InsuranceClaimsDetails();
        claimDetails.setTreatment(rowValidation.treatment());
        claimDetails.setTreatmentCategory(rowValidation.treatmentCategory());
        claimDetails.setFromTreatmentDate(rowValidation.fromDate());
        claimDetails.setToTreatmentDate(rowValidation.toDate());
        claimDetails.setDisease(IMPORTED_CLAIM_DISEASE);
        claimDetails.setInsuranceStaffCategoryPeriod(rowValidation.insurancePeriod());

        InsuranceClaimsRequest claim = new InsuranceClaimsRequest();
        claim.setRequestId(requestId);
        claim.setRequestAmount(rowValidation.claimAmount());
        claim.setRequestStatus(Workflow.APPROVED);
        claim.setRemark(trimToNull(rowValidation.remark()));
        claim.setInsuranceClaimsDetails(claimDetails);
        claim.setClaimsDependents(null);
        claim.setEmployee(rowValidation.employee());
        claim.setInsuranceDetailsLimit(rowValidation.insuranceDetailsLimit());
        claim.setInsuranceQuarter(rowValidation.insuranceQuarter());
        claim.setApprovalWorkFlows(new ArrayList<>(List.of(approvalWorkFlow)));
        claim.setApprovalLevel(ApprovalLevel.LEVEL03);
        claim.setApprovedAmount(rowValidation.approvedAmount());
        return insuranceClaimsRequestRepository.saveAndFlush(claim);
    }

    private ThirdPartyIndoorClaimImportRow mapBatchRow(RowValidation rowValidation, ThirdPartyIndoorClaimImportBatch batch) {
        ThirdPartyIndoorClaimImportRow row = new ThirdPartyIndoorClaimImportRow();
        row.setBatch(batch);
        row.setRowNo(rowValidation.rowNo());
        row.setExternalReferenceNo(rowValidation.externalReferenceNo());
        row.setCompanyCode(rowValidation.companyCode());
        row.setEpfNo(rowValidation.epfNo());
        row.setEmployeeName(rowValidation.employeeName());
        row.setPolicyYear(rowValidation.policyYear());
        row.setPolicyNo(rowValidation.policyNo());
        row.setFromDate(rowValidation.fromDate());
        row.setToDate(rowValidation.toDate());
        row.setIntimatedDate(rowValidation.intimatedDate());
        row.setPaidDate(rowValidation.paidDate());
        row.setNonPayableAmount(rowValidation.nonPayableAmount());
        row.setNonPayableItem(rowValidation.nonPayableItem());
        row.setClaimAmount(rowValidation.claimAmount());
        row.setApprovedAmount(rowValidation.approvedAmount());
        row.setRemark(rowValidation.remark());
        row.setErrorMessage(rowValidation.errors().isEmpty() ? null : String.join("; ", rowValidation.errors()));
        return row;
    }

    private ThirdPartyIndoorClaimBatchListResponseDTO mapBatchListResponse(ThirdPartyIndoorClaimImportBatch batch) {
        ThirdPartyIndoorClaimBatchListResponseDTO dto = new ThirdPartyIndoorClaimBatchListResponseDTO();
        dto.setId(batch.getId());
        dto.setBatchNo(batch.getBatchNo());
        dto.setFileName(batch.getFileName());
        dto.setFileType(batch.getFileType());
        dto.setStatus(batch.getStatus().name());
        dto.setStatusDescription(batch.getStatus().getDescription());
        dto.setTotalRows(batch.getTotalRows());
        dto.setValidRows(batch.getValidRows());
        dto.setInvalidRows(batch.getInvalidRows());
        dto.setDuplicateRows(batch.getDuplicateRows());
        dto.setImportedRows(batch.getImportedRows());
        dto.setCreatedDate(batch.getCreatedDate());
        dto.setCreatedBy(batch.getCreatedBy());
        return dto;
    }

    private ThirdPartyIndoorClaimBatchResponseDTO mapBatchResponse(ThirdPartyIndoorClaimImportBatch batch,
                                                                   List<ThirdPartyIndoorClaimImportRow> rows) {
        ThirdPartyIndoorClaimBatchResponseDTO dto = new ThirdPartyIndoorClaimBatchResponseDTO();
        dto.setId(batch.getId());
        dto.setBatchNo(batch.getBatchNo());
        dto.setFileName(batch.getFileName());
        dto.setFileType(batch.getFileType());
        dto.setStatus(batch.getStatus().name());
        dto.setStatusDescription(batch.getStatus().getDescription());
        dto.setTotalRows(batch.getTotalRows());
        dto.setValidRows(batch.getValidRows());
        dto.setInvalidRows(batch.getInvalidRows());
        dto.setDuplicateRows(batch.getDuplicateRows());
        dto.setImportedRows(batch.getImportedRows());
        dto.setCreatedDate(batch.getCreatedDate());
        dto.setCreatedBy(batch.getCreatedBy());
        dto.setRows(rows.stream().map(this::mapBatchRowResponse).toList());
        return dto;
    }

    private ThirdPartyIndoorClaimBatchRowResponseDTO mapBatchRowResponse(ThirdPartyIndoorClaimImportRow row) {
        ThirdPartyIndoorClaimBatchRowResponseDTO dto = new ThirdPartyIndoorClaimBatchRowResponseDTO();
        dto.setId(row.getId());
        dto.setRowNo(row.getRowNo());
        dto.setExternalReferenceNo(row.getExternalReferenceNo());
        dto.setCompanyCode(row.getCompanyCode());
        dto.setEpfNo(row.getEpfNo());
        dto.setEmployeeName(row.getEmployeeName());
        dto.setPolicyYear(row.getPolicyYear());
        dto.setPolicyNo(row.getPolicyNo());
        dto.setFromDate(row.getFromDate());
        dto.setToDate(row.getToDate());
        dto.setIntimatedDate(row.getIntimatedDate());
        dto.setPaidDate(row.getPaidDate());
        dto.setNonPayableAmount(row.getNonPayableAmount());
        dto.setNonPayableItem(row.getNonPayableItem());
        dto.setClaimAmount(row.getClaimAmount());
        dto.setApprovedAmount(row.getApprovedAmount());
        dto.setRemark(row.getRemark());
        dto.setStatus(row.getStatus() != null ? row.getStatus().name() : null);
        dto.setStatusDescription(row.getStatus() != null ? row.getStatus().getDescription() : null);
        dto.setErrorMessage(row.getErrorMessage());
        dto.setInsuranceClaimId(row.getInsuranceClaim() != null ? row.getInsuranceClaim().getId() : null);
        dto.setInsuranceClaimRequestId(row.getInsuranceClaim() != null ? row.getInsuranceClaim().getRequestId() : null);
        return dto;
    }

    private void createTemplateHeader(Workbook workbook, org.apache.poi.ss.usermodel.Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(TEMPLATE_HEADERS.get(i));
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }
    }

    private Map<String, Integer> resolveHeaderIndex(org.apache.poi.ss.usermodel.Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new IllegalArgumentException("Excel header row is missing");
        }

        DataFormatter formatter = new DataFormatter();
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String value = formatter.formatCellValue(cell);
            if (hasText(value)) {
                headerIndex.put(normalizeHeader(value), cell.getColumnIndex());
            }
        }

        List<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(header -> resolveColumnIndex(headerIndex, header) == null)
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException("Missing required columns: " + String.join(", ", missingHeaders));
        }

        return headerIndex;
    }

    private byte[] decodeFile(String file) {
        String normalized = file;
        int base64Separator = normalized.indexOf("base64,");
        if (base64Separator >= 0) {
            normalized = normalized.substring(base64Separator + 7);
        }
        return Base64.getDecoder().decode(normalized);
    }

    private String getString(Row row, Map<String, Integer> headerIndex, String header, DataFormatter formatter) {
        Integer columnIndex = resolveColumnIndex(headerIndex, header);
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        return trimToNull(formatter.formatCellValue(cell));
    }

    private Date getDate(Row row, Map<String, Integer> headerIndex, String header, DataFormatter formatter, List<String> errors) {
        String textValue = getString(row, headerIndex, header, formatter);
        if (!hasText(textValue)) {
            errors.add(header + " is required");
            return null;
        }

        Integer columnIndex = resolveColumnIndex(headerIndex, header);
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return DateTimeUtil.getStartOfDay(cell.getDateCellValue());
        }

        for (DateTimeFormatter dateFormatter : DATE_FORMATTERS) {
            try {
                LocalDate localDate = LocalDate.parse(textValue, dateFormatter);
                return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
                // Try next pattern.
            }
        }

        errors.add(header + " has an invalid date format");
        return null;
    }

    private Integer resolveColumnIndex(Map<String, Integer> headerIndex, String header) {
        for (String alias : HEADER_ALIASES.getOrDefault(header, List.of(header))) {
            Integer columnIndex = headerIndex.get(normalizeHeader(alias));
            if (columnIndex != null) {
                return columnIndex;
            }
        }
        return null;
    }

    private BigDecimal getBigDecimal(Row row, Map<String, Integer> headerIndex, String header, DataFormatter formatter, List<String> errors) {
        String textValue = getString(row, headerIndex, header, formatter);
        if (!hasText(textValue)) {
            errors.add(header + " is required");
            return null;
        }
        try {
            return new BigDecimal(textValue.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            errors.add(header + " must be a valid number");
            return null;
        }
    }

    private BigDecimal getOptionalBigDecimal(Row row, Map<String, Integer> headerIndex, String header, DataFormatter formatter, List<String> errors) {
        String textValue = getString(row, headerIndex, header, formatter);
        if (!hasText(textValue)) {
            return null;
        }
        try {
            return new BigDecimal(textValue.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            errors.add(header + " must be a valid number");
            return null;
        }
    }

    private Integer getInteger(Row row, Map<String, Integer> headerIndex, String header, DataFormatter formatter, List<String> errors) {
        String textValue = getString(row, headerIndex, header, formatter);
        if (!hasText(textValue)) {
            return null;
        }
        try {
            return Integer.parseInt(textValue.trim());
        } catch (NumberFormatException ex) {
            errors.add(header + " must be a valid whole number");
            return null;
        }
    }

    private boolean isDateWithinPeriod(Date date, InsuranceStaffCategoryPeriod period) {
        if (date == null || period == null || period.getFromDate() == null || period.getToDate() == null) {
            return false;
        }
        return !date.before(period.getFromDate()) && !date.after(period.getToDate());
    }

    private boolean isBlankRow(Row row) {
        if (row == null) {
            return true;
        }
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : row) {
            if (hasText(formatter.formatCellValue(cell))) {
                return false;
            }
        }
        return true;
    }

    private String buildEmployeeName(ApplicationUser employee) {
        if (employee == null || employee.getUserPersonalDetails() == null) {
            return null;
        }
        return (Optional.ofNullable(employee.getUserPersonalDetails().getFirstName()).orElse("") + " "
                + Optional.ofNullable(employee.getUserPersonalDetails().getLastName()).orElse("")).trim();
    }

    private List<String> buildImportValidationErrors(ValidationResult validationResult) {
        return validationResult.rows().stream()
                .filter(row -> row.status() == ValidationStatus.FAILED)
                .limit(10)
                .flatMap(row -> row.errors().stream().map(error -> "Row " + row.rowNo() + ": " + error))
                .toList();
    }

    private String generateBatchNo(Long id) {
        return "TPIC/" + DateTimeUtil.getCurrentYear() + "/" + String.format("%04d", id);
    }

    private String normalizeReference(String value) {
        return trimToNull(value) != null ? value.trim().toLowerCase() : null;
    }

    private String normalizeHeader(String value) {
        return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private enum ValidationStatus {
        VALID,
        DUPLICATE,
        FAILED
    }

    private record RowValidation(
            Integer rowNo,
            String externalReferenceNo,
            String companyCode,
            String epfNo,
            String employeeName,
            Integer policyYear,
            String policyNo,
            Date fromDate,
            Date toDate,
            Date intimatedDate,
            Date paidDate,
            BigDecimal nonPayableAmount,
            String nonPayableItem,
            BigDecimal claimAmount,
            BigDecimal approvedAmount,
            String remark,
            ValidationStatus status,
            List<String> errors,
            ApplicationUser employee,
            InsuranceStaffCategoryPeriod insurancePeriod,
            InsuranceDetailsLimit insuranceDetailsLimit,
            InsuranceQuarter insuranceQuarter,
            Treatment treatment,
            com.dtech.admin.model.TreatmentCategory treatmentCategory
    ) {
        private ThirdPartyIndoorClaimValidationRowResponseDTO toResponse() {
            ThirdPartyIndoorClaimValidationRowResponseDTO dto = new ThirdPartyIndoorClaimValidationRowResponseDTO();
            dto.setRowNo(rowNo);
            dto.setExternalReferenceNo(externalReferenceNo);
            dto.setCompanyCode(companyCode);
            dto.setEpfNo(epfNo);
            dto.setEmployeeName(employeeName);
            dto.setPolicyYear(policyYear);
            dto.setPolicyNo(policyNo);
            dto.setFromDate(fromDate);
            dto.setToDate(toDate);
            dto.setIntimatedDate(intimatedDate);
            dto.setPaidDate(paidDate);
            dto.setNonPayableAmount(nonPayableAmount);
            dto.setNonPayableItem(nonPayableItem);
            dto.setClaimAmount(claimAmount);
            dto.setApprovedAmount(approvedAmount);
            dto.setRemark(remark);
            dto.setStatus(status.name());
            dto.setErrors(errors);
            return dto;
        }
    }

    private record ValidationResult(String fileName, List<RowValidation> rows) {
        private int totalRows() {
            return rows.size();
        }

        private int validRows() {
            return (int) rows.stream().filter(row -> row.status() == ValidationStatus.VALID).count();
        }

        private int invalidRows() {
            return (int) rows.stream().filter(row -> row.status() == ValidationStatus.FAILED).count();
        }

        private int duplicateRows() {
            return (int) rows.stream().filter(row -> row.status() == ValidationStatus.DUPLICATE).count();
        }

        private ThirdPartyIndoorClaimValidationResponseDTO toResponse() {
            ThirdPartyIndoorClaimValidationResponseDTO dto = new ThirdPartyIndoorClaimValidationResponseDTO();
            dto.setFileName(fileName);
            dto.setTotalRows(totalRows());
            dto.setValidRows(validRows());
            dto.setInvalidRows(invalidRows());
            dto.setDuplicateRows(duplicateRows());
            dto.setRows(rows.stream().map(RowValidation::toResponse).collect(Collectors.toList()));
            return dto;
        }
    }
}


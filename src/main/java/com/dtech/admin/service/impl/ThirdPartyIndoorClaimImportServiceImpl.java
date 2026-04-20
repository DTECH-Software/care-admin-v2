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
    private static final String TEMPLATE_FILE_NAME = "third-party-indoor-claims-template.xlsx";
    private static final List<String> TEMPLATE_HEADERS = List.of(
            "thirdPartyReferenceNo",
            "claimantType",
            "epfNo",
            "employeeNic",
            "dependentNic",
            "dependentRelation",
            "fromDate",
            "toDate",
            "hospital",
            "disease",
            "requestAmount",
            "approvedAmount",
            "remark"
    );
    private static final List<String> REQUIRED_HEADERS = List.of(
            "thirdPartyReferenceNo",
            "claimantType",
            "epfNo",
            "employeeNic",
            "fromDate",
            "toDate",
            "disease",
            "requestAmount",
            "approvedAmount"
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
    private final ClaimDependentsRepository claimDependentsRepository;

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
            responseMap.put("claimantTypes", Arrays.stream(ThirdPartyIndoorClaimClaimantType.values())
                    .map(type -> new SimpleBaseDTO(type.name(), type.getDescription()))
                    .toList());
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
                    "Dependent rows must provide dependentNic.",
                    "hospital is optional."
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
        String claimantTypeText = getString(row, headerIndex, "claimantType", formatter);
        String epfNo = getString(row, headerIndex, "epfNo", formatter);
        String employeeNic = getString(row, headerIndex, "employeeNic", formatter);
        String dependentNic = getString(row, headerIndex, "dependentNic", formatter);
        String dependentRelation = getString(row, headerIndex, "dependentRelation", formatter);
        String hospital = getString(row, headerIndex, "hospital", formatter);
        String disease = getString(row, headerIndex, "disease", formatter);
        String remark = getString(row, headerIndex, "remark", formatter);

        Date fromDate = getDate(row, headerIndex, "fromDate", formatter, errors);
        Date toDate = getDate(row, headerIndex, "toDate", formatter, errors);
        BigDecimal requestAmount = getBigDecimal(row, headerIndex, "requestAmount", formatter, errors);
        BigDecimal approvedAmount = getBigDecimal(row, headerIndex, "approvedAmount", formatter, errors);

        if (!hasText(externalReferenceNo)) {
            errors.add("thirdPartyReferenceNo is required");
        }

        String normalizedReference = normalizeReference(externalReferenceNo);
        if (hasText(normalizedReference) && !fileReferences.add(normalizedReference)) {
            errors.add("Duplicate thirdPartyReferenceNo found in the uploaded file");
        }

        ThirdPartyIndoorClaimClaimantType claimantType = null;
        if (!hasText(claimantTypeText)) {
            errors.add("claimantType is required");
        } else {
            try {
                claimantType = ThirdPartyIndoorClaimClaimantType.valueOf(claimantTypeText.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                errors.add("claimantType is invalid");
            }
        }

        if (!hasText(epfNo)) {
            errors.add("epfNo is required");
        }

        if (!hasText(employeeNic)) {
            errors.add("employeeNic is required");
        }

        if (!hasText(disease)) {
            errors.add("disease is required");
        }

        if (fromDate != null && toDate != null && fromDate.after(toDate)) {
            errors.add("fromDate cannot be after toDate");
        }

        if (requestAmount != null && requestAmount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("requestAmount must be greater than zero");
        }

        if (approvedAmount != null && approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("approvedAmount must be greater than zero");
        }

        if (requestAmount != null && approvedAmount != null && approvedAmount.compareTo(requestAmount) > 0) {
            errors.add("approvedAmount cannot be greater than requestAmount");
        }

        ApplicationUser employee = null;
        ClaimsDependents dependent = null;
        String employeeName = null;
        String dependentName = null;
        InsuranceStaffCategoryPeriod insurancePeriod = null;
        InsuranceDetailsLimit insuranceDetailsLimit = null;
        InsuranceQuarter insuranceQuarter = null;

        if (errors.isEmpty()) {
            employee = applicationUserRepository
                    .findByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatus(
                            epfNo.trim(), employeeNic.trim(), Status.ACTIVE)
                    .orElse(null);

            if (employee == null) {
                errors.add("Employee not found for the given epfNo and employeeNic");
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
                } else {
                    insurancePeriod = insuranceStaffCategoryPeriodRepository.findByDateWithinRange(
                            toDate, companyDetails.getStaffCategories().getCode()).orElse(null);
                    if (insurancePeriod == null) {
                        errors.add("Insurance period not found for the treatment date");
                    } else {
                        insuranceDetailsLimit = insuranceDetailsLimitRepository.findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment(
                                companyDetails.getInsurancePolicy(), Status.ACTIVE, insurancePeriod, treatment).orElse(null);
                        if (insuranceDetailsLimit == null) {
                            errors.add("Indoor insurance limit is not configured for the employee policy");
                        } else if (Boolean.TRUE.equals(insuranceDetailsLimit.getIsQuarter())) {
                            insuranceQuarter = insuranceQuarterRepository.findByDateWithinRangeAndCodeWithLimit(
                                    insuranceDetailsLimit, FIXED_TREATMENT_CATEGORY_CODE, toDate).orElse(null);
                        }
                    }
                }
            }
        }

        if (errors.isEmpty() && claimantType == ThirdPartyIndoorClaimClaimantType.DEPENDENT) {
            if (!hasText(dependentNic)) {
                errors.add("dependentNic is required for dependent rows");
            } else {
                dependent = claimDependentsRepository
                        .findFirstByApplicationUserAndNicIgnoreCaseAndStatusAndEligibleFacilityInAndLiveStatus(
                                employee, dependentNic.trim(), Workflow.APPROVED, INSURANCE_FACILITIES, true)
                        .orElse(null);

                if (dependent == null) {
                    errors.add("Dependent not found or not eligible for insurance claims");
                } else {
                    dependentName = buildDependentName(dependent);
                    if (hasText(dependentRelation)
                            && dependent.getRelationCategory() != null
                            && !dependent.getRelationCategory().name().equalsIgnoreCase(dependentRelation.trim())) {
                        errors.add("dependentRelation does not match the existing dependent");
                    }
                }
            }
        }

        if (errors.isEmpty() && rowRepository.existsByExternalReferenceNoIgnoreCaseAndInsuranceClaimIsNotNull(externalReferenceNo.trim())) {
            return new RowValidation(rowNo, externalReferenceNo.trim(), claimantType, epfNo, employeeNic, employeeName,
                    dependentNic, dependentName, dependentRelation, fromDate, toDate, hospital, disease,
                    requestAmount, approvedAmount, remark, ValidationStatus.DUPLICATE, List.of(),
                    employee, dependent, insurancePeriod, insuranceDetailsLimit, insuranceQuarter, treatment, treatmentCategory);
        }

        ValidationStatus status = errors.isEmpty() ? ValidationStatus.VALID : ValidationStatus.FAILED;
        return new RowValidation(rowNo, trimToNull(externalReferenceNo), claimantType, trimToNull(epfNo), trimToNull(employeeNic),
                employeeName, trimToNull(dependentNic), dependentName, trimToNull(dependentRelation),
                fromDate, toDate, trimToNull(hospital), trimToNull(disease), requestAmount, approvedAmount,
                trimToNull(remark), status, errors, employee, dependent, insurancePeriod, insuranceDetailsLimit,
                insuranceQuarter, treatment, treatmentCategory);
    }

    private InsuranceClaimsRequest createImportedClaim(RowValidation rowValidation, String username) {
        ClaimRequestIdGen claimRequestIdGen = ClaimRequestIdGen.builder()
                .year(String.valueOf(DateTimeUtil.getYear(rowValidation.toDate())))
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
        claimDetails.setDisease(rowValidation.disease());
        claimDetails.setInsuranceStaffCategoryPeriod(rowValidation.insurancePeriod());

        InsuranceClaimsRequest claim = new InsuranceClaimsRequest();
        claim.setRequestId(requestId);
        claim.setRequestAmount(rowValidation.requestAmount());
        claim.setRequestStatus(Workflow.APPROVED);
        claim.setRemark(trimToNull(rowValidation.remark()));
        claim.setInsuranceClaimsDetails(claimDetails);
        claim.setClaimsDependents(rowValidation.dependent());
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
        row.setClaimantType(rowValidation.claimantType());
        row.setEpfNo(rowValidation.epfNo());
        row.setEmployeeNic(rowValidation.employeeNic());
        row.setEmployeeName(rowValidation.employeeName());
        row.setDependentNic(rowValidation.dependentNic());
        row.setDependentName(rowValidation.dependentName());
        row.setDependentRelation(rowValidation.dependentRelation());
        row.setFromDate(rowValidation.fromDate());
        row.setToDate(rowValidation.toDate());
        row.setHospital(rowValidation.hospital());
        row.setDisease(rowValidation.disease());
        row.setRequestAmount(rowValidation.requestAmount());
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
        dto.setClaimantType(row.getClaimantType() != null ? row.getClaimantType().name() : null);
        dto.setClaimantTypeDescription(row.getClaimantType() != null ? row.getClaimantType().getDescription() : null);
        dto.setEpfNo(row.getEpfNo());
        dto.setEmployeeNic(row.getEmployeeNic());
        dto.setEmployeeName(row.getEmployeeName());
        dto.setDependentNic(row.getDependentNic());
        dto.setDependentName(row.getDependentName());
        dto.setDependentRelation(row.getDependentRelation());
        dto.setFromDate(row.getFromDate());
        dto.setToDate(row.getToDate());
        dto.setHospital(row.getHospital());
        dto.setDisease(row.getDisease());
        dto.setRequestAmount(row.getRequestAmount());
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
                .map(this::normalizeHeader)
                .filter(header -> !headerIndex.containsKey(header))
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
        Integer columnIndex = headerIndex.get(normalizeHeader(header));
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

        Integer columnIndex = headerIndex.get(normalizeHeader(header));
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

    private String buildDependentName(ClaimsDependents dependent) {
        if (dependent == null) {
            return null;
        }
        return (Optional.ofNullable(dependent.getFirstName()).orElse("") + " "
                + Optional.ofNullable(dependent.getLastName()).orElse("")).trim();
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
            ThirdPartyIndoorClaimClaimantType claimantType,
            String epfNo,
            String employeeNic,
            String employeeName,
            String dependentNic,
            String dependentName,
            String dependentRelation,
            Date fromDate,
            Date toDate,
            String hospital,
            String disease,
            BigDecimal requestAmount,
            BigDecimal approvedAmount,
            String remark,
            ValidationStatus status,
            List<String> errors,
            ApplicationUser employee,
            ClaimsDependents dependent,
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
            dto.setClaimantType(claimantType != null ? claimantType.name() : null);
            dto.setClaimantTypeDescription(claimantType != null ? claimantType.getDescription() : null);
            dto.setEpfNo(epfNo);
            dto.setEmployeeNic(employeeNic);
            dto.setEmployeeName(employeeName);
            dto.setDependentNic(dependentNic);
            dto.setDependentName(dependentName);
            dto.setDependentRelation(dependentRelation);
            dto.setFromDate(fromDate);
            dto.setToDate(toDate);
            dto.setHospital(hospital);
            dto.setDisease(disease);
            dto.setRequestAmount(requestAmount);
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


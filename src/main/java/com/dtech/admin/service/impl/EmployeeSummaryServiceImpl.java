package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeSummaryClaimViewRequestDTO;
import com.dtech.admin.dto.request.EmployeeSummaryRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.EmployeeSummaryApprovalHistoryDTO;
import com.dtech.admin.dto.response.EmployeeSummaryBalanceRowDTO;
import com.dtech.admin.dto.response.EmployeeSummaryClaimInfoDTO;
import com.dtech.admin.dto.response.EmployeeSummaryClaimRowDTO;
import com.dtech.admin.dto.response.EmployeeSummaryClaimViewResponseDTO;
import com.dtech.admin.dto.response.EmployeeSummaryEmployeeResponseDTO;
import com.dtech.admin.dto.search.EmployeeSummarySearchDTO;
import com.dtech.admin.enums.AuditTask;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.TreatmentType;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.InsuranceDetailsLimit;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.repository.ApplicationUserRepository;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.InsuranceClaimsRequestRepository;
import com.dtech.admin.repository.InsuranceDetailsLimitRepository;
import com.dtech.admin.repository.InsuranceStaffCategoryPeriodRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.EmployeeSummaryService;
import com.dtech.admin.specifications.EmployeeSummarySpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ApprovalRemarkUtil;
import com.dtech.admin.util.ResponseMessageUtil;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmployeeSummaryServiceImpl implements EmployeeSummaryService {

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final InsuranceStaffCategoryPeriodRepository insuranceStaffCategoryPeriodRepository;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final InsuranceDetailsLimitRepository insuranceDetailsLimitRepository;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final Gson gson;

    @Autowired
    private final RejoinCarryForwardService rejoinCarryForwardService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Employee summary reference data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter
                    .getPrivileges(channelRequestDTO.getUsername(), WebPage.SUMM_EMPV.name());

            List<SimpleBaseDTO> companyTypes = companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                    .map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription()))
                    .toList();

            List<SimpleBaseDTO> period = insuranceStaffCategoryPeriodRepository.findAll().stream()
                    .map(val -> new SimpleBaseDTO(String.valueOf(val.getId()),
                            val.getFromDate() + " to " + val.getToDate() + " " + val.getStaffCategories().getDescription()))
                    .toList();

            responseMap.put("privileges", privileges);
            responseMap.put("company", companyTypes);
            responseMap.put("period", period);

            auditLogService.log(WebPage.SUMM_EMPV.name(), WebTask.REF_DATA.name(),
                    AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(),
                    channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(responseMap,
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_SUMMARY_REFERENCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load employee summary reference data", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getEmployeeName(EmployeeSummaryRequestDTO requestDTO, Locale locale) {
        try {
            log.info("Employee summary employee lookup {}", requestDTO);
            Optional<ApplicationUser> employeeOpt = findEmployee(requestDTO.getCompany(), requestDTO.getEpfNo());
            if (employeeOpt.isEmpty()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 404,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_SUMMARY_EMPLOYEE_NOT_FOUND, null, locale)));
            }

            if (requestDTO.getPeriodId() != null) {
                Optional<InsuranceStaffCategoryPeriod> periodOpt = insuranceStaffCategoryPeriodRepository.findById(requestDTO.getPeriodId());
                if (periodOpt.isEmpty()) {
                    return ResponseEntity.ok().body(responseUtil.error(null, 404,
                            messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_SUMMARY_PERIOD_NOT_FOUND, null, locale)));
                }
            }

            ApplicationUser employee = employeeOpt.get();
            String name = buildEmployeeName(employee);

            EmployeeSummaryEmployeeResponseDTO responseDTO = new EmployeeSummaryEmployeeResponseDTO();
            responseDTO.setEpfNo(employee.getUserPersonalDetails().getEpfNo());
            responseDTO.setEmployeeName(name);

            auditLogService.log(WebPage.SUMM_EMPV.name(), WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), requestDTO.getIp(),
                    requestDTO.getUserAgent(), gson.toJson(responseDTO), null, requestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_SUMMARY_EMPLOYEE_NAME_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to lookup employee name", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeSummarySearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Employee summary filter list {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            EmployeeSummarySearchDTO search = Optional.ofNullable(paginationRequest.getSearch())
                    .orElseGet(EmployeeSummarySearchDTO::new);

            Page<InsuranceClaimsRequest> page = insuranceClaimsRequestRepository.findAll(
                    EmployeeSummarySpecification.getSpecification(search), pageable);
            long total = insuranceClaimsRequestRepository.count(EmployeeSummarySpecification.getSpecification(search));

            List<EmployeeSummaryClaimRowDTO> rows = page.stream()
                    .map(this::mapClaimRow)
                    .toList();

            PagingResult<EmployeeSummaryClaimRowDTO> result = new PagingResult<>(rows, rows.size(), total);

            List<EmployeeSummaryBalanceRowDTO> balances = resolveBalances(search);
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("claims", result);
            responseMap.put("balances", balances);

            auditLogService.log(WebPage.SUMM_EMPV.name(), WebTask.SEARCH.name(),
                    AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(),
                    paginationRequest.getUserAgent(), gson.toJson(responseMap), null, paginationRequest.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseMap,
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_SUMMARY_FILTER_LIST_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to filter employee summary claims", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getTreatmentBalances(EmployeeSummaryRequestDTO requestDTO, Locale locale) {
        try {
            log.info("Employee summary treatment balances {}", requestDTO);
            Optional<ApplicationUser> employeeOpt = findEmployee(requestDTO.getCompany(), requestDTO.getEpfNo());
            if (employeeOpt.isEmpty()) {
                return ResponseEntity.ok().body(responseUtil.error(null, 404,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_SUMMARY_EMPLOYEE_NOT_FOUND, null, locale)));
            }

            InsuranceStaffCategoryPeriod period = insuranceStaffCategoryPeriodRepository.findById(requestDTO.getPeriodId())
                    .orElse(null);
            if (period == null) {
                return ResponseEntity.ok().body(responseUtil.error(null, 404,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_SUMMARY_PERIOD_NOT_FOUND, null, locale)));
            }

            ApplicationUser employee = employeeOpt.get();
            List<EmployeeSummaryBalanceRowDTO> rows = buildBalanceRows(employee, period);

            auditLogService.log(WebPage.SUMM_EMPV.name(), WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), requestDTO.getIp(),
                    requestDTO.getUserAgent(), gson.toJson(rows), null, requestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) rows,
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_SUMMARY_BALANCE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to load treatment balances", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(EmployeeSummaryClaimViewRequestDTO requestDTO, Locale locale) {
        try {
            log.info("Employee summary view {}", requestDTO);
            InsuranceClaimsRequest claim = insuranceClaimsRequestRepository.findById(requestDTO.getId())
                    .orElse(null);
            if (claim == null) {
                return ResponseEntity.ok().body(responseUtil.error(null, 404,
                        messageSource.getMessage(ResponseMessageUtil.CLAIMS_DETAILS_NOT_FOUND, new Object[]{requestDTO.getId()}, locale)));
            }

            EmployeeSummaryClaimViewResponseDTO responseDTO = new EmployeeSummaryClaimViewResponseDTO();
            responseDTO.setClaim(mapClaimInfo(claim));
            responseDTO.setApprovalHistory(mapApprovalHistory(claim.getApprovalWorkFlows()));

            InsuranceStaffCategoryPeriod period = claim.getInsuranceClaimsDetails() != null
                    ? claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod()
                    : null;
            responseDTO.setBalances(resolveBalances(claim.getEmployee(), period));

            auditLogService.log(WebPage.SUMM_EMPV.name(), WebTask.VIEW.name(),
                    AuditTask.VIEW_DATA.getDescription(), requestDTO.getIp(),
                    requestDTO.getUserAgent(), gson.toJson(responseDTO), null, requestDTO.getUsername());

            return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_SUMMARY_VIEW_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Failed to view employee summary", e);
            throw e;
        }
    }

    private Optional<ApplicationUser> findEmployee(String company, String epfNo) {
        if (!hasText(company) || !hasText(epfNo)) {
            return Optional.empty();
        }
        return applicationUserRepository
                .findByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_UserCompanyDetails_CompanyTypes_Code(
                        epfNo, company);
    }

    private List<EmployeeSummaryBalanceRowDTO> resolveBalances(EmployeeSummarySearchDTO search) {
        if (search == null || !hasText(search.getCompany()) || !hasText(search.getEpfNo()) || search.getPeriodId() == null) {
            return List.of();
        }
        Optional<ApplicationUser> employeeOpt = findEmployee(search.getCompany(), search.getEpfNo());
        if (employeeOpt.isEmpty()) {
            return List.of();
        }
        InsuranceStaffCategoryPeriod period = insuranceStaffCategoryPeriodRepository.findById(search.getPeriodId())
                .orElse(null);
        if (period == null) {
            return List.of();
        }
        return buildBalanceRows(employeeOpt.get(), period);
    }

    private List<EmployeeSummaryBalanceRowDTO> resolveBalances(ApplicationUser employee,
                                                               InsuranceStaffCategoryPeriod period) {
        if (employee == null || period == null) {
            return List.of();
        }
        return buildBalanceRows(employee, period);
    }

    private EmployeeSummaryClaimRowDTO mapClaimRow(InsuranceClaimsRequest claim) {
        EmployeeSummaryClaimRowDTO dto = new EmployeeSummaryClaimRowDTO();
        dto.setId(claim.getId());
        dto.setRequestId(claim.getRequestId());
        if (claim.getInsuranceClaimsDetails() != null && claim.getInsuranceClaimsDetails().getTreatment() != null) {
            dto.setTreatmentType(claim.getInsuranceClaimsDetails().getTreatment().getTreatmentDescription());
        }
        if (claim.getInsuranceClaimsDetails() != null && claim.getInsuranceClaimsDetails().getTreatmentCategory() != null) {
            dto.setTreatmentCategory(claim.getInsuranceClaimsDetails().getTreatmentCategory().getDescription());
        }
        dto.setSubmittedValue(claim.getRequestAmount());
        dto.setAppliedDate(claim.getCreatedDate());
        dto.setApprovedValue(claim.getApprovedAmount());
        dto.setRemark(resolveClaimRemark(claim));
        return dto;
    }

    private EmployeeSummaryClaimInfoDTO mapClaimInfo(InsuranceClaimsRequest claim) {
        EmployeeSummaryClaimInfoDTO dto = new EmployeeSummaryClaimInfoDTO();
        dto.setId(claim.getId());
        if (claim.getInsuranceClaimsDetails() != null) {
            if (claim.getInsuranceClaimsDetails().getTreatment() != null) {
                dto.setTreatment(claim.getInsuranceClaimsDetails().getTreatment().getTreatmentDescription());
            }
            if (claim.getInsuranceClaimsDetails().getTreatmentCategory() != null) {
                dto.setTreatmentCategory(claim.getInsuranceClaimsDetails().getTreatmentCategory().getDescription());
            }
        }
        dto.setSubmittedValue(claim.getRequestAmount());
        dto.setAppliedDate(claim.getCreatedDate());
        dto.setApprovedValue(claim.getApprovedAmount());
        if (claim.getRequestStatus() != null) {
            dto.setStatus(claim.getRequestStatus().name());
            dto.setStatusDescription(claim.getRequestStatus().getDescription());
        }
        return dto;
    }

    private List<EmployeeSummaryApprovalHistoryDTO> mapApprovalHistory(List<ApprovalWorkFlow> workflows) {
        if (workflows == null || workflows.isEmpty()) {
            return List.of();
        }
        return workflows.stream()
                .sorted(Comparator.comparing(ApprovalWorkFlow::getApprovedDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(flow -> {
                    EmployeeSummaryApprovalHistoryDTO dto = new EmployeeSummaryApprovalHistoryDTO();
                    if (flow.getApprovalLevel() != null) {
                        dto.setApprovalLevel(flow.getApprovalLevel().name());
                        dto.setApprovalLevelDescription(flow.getApprovalLevel().getDescription());
                    }
                    dto.setApprovedDate(flow.getApprovedDate());
                    dto.setApprovedUser(flow.getApprovedUser());
                    if (flow.getStatus() != null) {
                        dto.setStatus(flow.getStatus().name());
                        dto.setStatusDescription(flow.getStatus().getDescription());
                    }
                    dto.setRejectedRemark(flow.getRejectedRemark());
                    dto.setApprovedAmount(flow.getApprovedAmount());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private String buildEmployeeName(ApplicationUser employee) {
        if (employee == null || employee.getUserPersonalDetails() == null) {
            return "";
        }
        String firstName = Objects.toString(employee.getUserPersonalDetails().getFirstName(), "");
        String lastName = Objects.toString(employee.getUserPersonalDetails().getLastName(), "");
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? firstName + lastName : fullName;
    }

    private String resolveClaimRemark(InsuranceClaimsRequest claim) {
        return ApprovalRemarkUtil.resolveLevelTwoOrThreeRemark(claim);
    }

    private String extractLatestWorkflowRemark(List<ApprovalWorkFlow> approvalWorkFlows) {
        return ApprovalRemarkUtil.resolveLevelTwoOrThreeRemark(approvalWorkFlows);
    }

    private EmployeeSummaryBalanceRowDTO buildBalanceRow(InsuranceDetailsLimit limit,
                                                         com.dtech.admin.model.InsuranceQuarter quarter,
                                                         BigDecimal fundLimit,
                                                         BigDecimal availableLimit) {
        EmployeeSummaryBalanceRowDTO dto = new EmployeeSummaryBalanceRowDTO();
        dto.setTreatmentCode(limit.getTreatment().getTreatmentCode());
        dto.setTreatmentDescription(limit.getTreatment().getTreatmentDescription());
        dto.setTreatmentCategoryCode(quarter.getTreatmentCategory().getCode());
        dto.setTreatmentCategoryDescription(quarter.getTreatmentCategory().getDescription());
        dto.setFundLimit(fundLimit);
        dto.setAvailableLimit(availableLimit);
        return dto;
    }

    private List<EmployeeSummaryBalanceRowDTO> buildBalanceRows(ApplicationUser employee,
                                                                InsuranceStaffCategoryPeriod period) {
        List<InsuranceDetailsLimit> limits = insuranceDetailsLimitRepository
                .findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriod(
                        employee.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                        Status.ACTIVE,
                        period);

        java.util.Date permanentDate = rejoinCarryForwardService.resolveEffectivePermanentDateForLimit(employee);
        return limits.stream()
                .flatMap(limit -> {
                    List<com.dtech.admin.model.InsuranceQuarter> quarters = limit.getInsuranceQuarters();
                    if (quarters == null || quarters.isEmpty()) {
                        return java.util.stream.Stream.empty();
                    }

                    com.dtech.admin.model.InsuranceQuarter referenceQuarter =
                            selectQuarterByPermanentDate(quarters, permanentDate);
                    java.util.Date rangeFrom = referenceQuarter != null ? referenceQuarter.getFromDate() : null;
                    java.util.Date rangeTo = referenceQuarter != null ? referenceQuarter.getToDate() : null;

                    Map<String, List<com.dtech.admin.model.InsuranceQuarter>> byCategory = quarters.stream()
                            .filter(q -> q.getTreatmentCategory() != null)
                            .collect(Collectors.groupingBy(q -> q.getTreatmentCategory().getCode()));

                    Map<String, com.dtech.admin.model.InsuranceQuarter> categoryQuarterMap = byCategory.values().stream()
                            .map(list -> {
                                List<com.dtech.admin.model.InsuranceQuarter> sorted = list.stream()
                                        .sorted(Comparator.comparing(com.dtech.admin.model.InsuranceQuarter::getFromDate,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                        .toList();
                                com.dtech.admin.model.InsuranceQuarter categoryQuarter = matchQuarterRange(sorted, rangeFrom, rangeTo);
                                if (categoryQuarter == null && !sorted.isEmpty()) {
                                    categoryQuarter = sorted.get(0);
                                }
                                return categoryQuarter;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toMap(
                                    q -> q.getTreatmentCategory().getCode(),
                                    q -> q,
                                    (a, b) -> a));

                    if (categoryQuarterMap.isEmpty()) {
                        return java.util.stream.Stream.empty();
                    }

                    java.util.Date previousPermanentDate = employee.getUserPersonalDetails()
                            .getUserCompanyDetails()
                            .getPreviousPermanentDate();
                    java.util.Date changeDate = previousPermanentDate != null
                            ? employee.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate()
                            : null;
                    InsuranceStaffCategoryPeriod prevPeriod = null;
                    if (changeDate != null && period.getStaffCategories() != null) {
                        prevPeriod = insuranceStaffCategoryPeriodRepository
                                .findByDateWithinRangeAnyStaff(changeDate)
                                .stream()
                                .filter(p -> p.getStaffCategories() != null)
                                .filter(p -> !p.getStaffCategories().getCode()
                                        .equals(period.getStaffCategories().getCode()))
                                .findFirst()
                                .orElse(null);
                    }
                    BigDecimal treatmentFundLimit = categoryQuarterMap.values().stream()
                            .map(q -> {
                                if (limit.getIsQuarter() != null && !limit.getIsQuarter()) {
                                    return limit.getGlobalLimit();
                                }
                                return q.getQuarterLimit() != null ? q.getQuarterLimit() : limit.getGlobalLimit();
                            })
                            .filter(Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .orElse(BigDecimal.ZERO);

                    BigDecimal treatmentApprovedAmount = rejoinCarryForwardService.getApprovedAmountByTreatment(
                            employee,
                            limit.getTreatment().getTreatmentCode(),
                            period.getId(),
                            prevPeriod
                    );
                    BigDecimal treatmentRemaining = subtractToZero(treatmentFundLimit, treatmentApprovedAmount);
                    final BigDecimal finalTreatmentRemaining = treatmentRemaining;

                    return categoryQuarterMap.values().stream()
                            .map(list -> {
                                com.dtech.admin.model.InsuranceQuarter categoryQuarter = list;
                                BigDecimal fundLimit = (limit.getIsQuarter() != null && !limit.getIsQuarter())
                                        ? limit.getGlobalLimit()
                                        : (categoryQuarter.getQuarterLimit() != null
                                        ? categoryQuarter.getQuarterLimit()
                                        : limit.getGlobalLimit());
                                if (fundLimit == null) {
                                    return null;
                                }
                                BigDecimal categoryApprovedAmount = rejoinCarryForwardService.getApprovedAmountByTreatmentCategory(
                                        employee,
                                        limit.getTreatment().getTreatmentCode(),
                                        categoryQuarter.getTreatmentCategory().getCode(),
                                        period.getId(),
                                        prevPeriod
                                );
                                BigDecimal categoryRemaining = subtractToZero(fundLimit, categoryApprovedAmount);
                                BigDecimal availableLimit = finalTreatmentRemaining.min(categoryRemaining);
                                return buildBalanceRow(limit, categoryQuarter, fundLimit, availableLimit);
                            })
                            .filter(Objects::nonNull);
                })
                .toList();
    }

    private com.dtech.admin.model.InsuranceQuarter selectQuarterByPermanentDate(
            List<com.dtech.admin.model.InsuranceQuarter> quarters,
            java.util.Date permanentDate) {
        if (quarters == null || quarters.isEmpty()) {
            return null;
        }
        List<com.dtech.admin.model.InsuranceQuarter> sorted = quarters.stream()
                .sorted(Comparator.comparing(com.dtech.admin.model.InsuranceQuarter::getFromDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        com.dtech.admin.model.InsuranceQuarter first = sorted.get(0);
        if (permanentDate == null || first.getFromDate() == null || first.getToDate() == null) {
            return first;
        }
        if (permanentDate.before(first.getFromDate())) {
            return first;
        }
        for (com.dtech.admin.model.InsuranceQuarter quarter : sorted) {
            if (quarter.getFromDate() == null || quarter.getToDate() == null) {
                continue;
            }
            if (!permanentDate.before(quarter.getFromDate()) && !permanentDate.after(quarter.getToDate())) {
                return quarter;
            }
        }
        return sorted.get(sorted.size() - 1);
    }

    private com.dtech.admin.model.InsuranceQuarter matchQuarterRange(
            List<com.dtech.admin.model.InsuranceQuarter> quarters,
            java.util.Date rangeFrom,
            java.util.Date rangeTo) {
        if (quarters == null || quarters.isEmpty() || rangeFrom == null || rangeTo == null) {
            return null;
        }
        return quarters.stream()
                .filter(q -> q.getFromDate() != null && q.getToDate() != null)
                .filter(q -> q.getFromDate().equals(rangeFrom) && q.getToDate().equals(rangeTo))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal subtractToZero(BigDecimal fundLimit, BigDecimal usedAmount) {
        BigDecimal safeFundLimit = fundLimit != null ? fundLimit : BigDecimal.ZERO;
        BigDecimal safeUsedAmount = usedAmount != null ? usedAmount : BigDecimal.ZERO;
        BigDecimal remaining = safeFundLimit.subtract(safeUsedAmount);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    // removed per-row global-limit calculation; handled in buildBalanceRows with treatment-level pool

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}

package com.dtech.admin.service.impl;

import com.dtech.admin.dto.AvailableInsuranceLimitDTO;
import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ClaimRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.*;
import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.enums.TreatmentCategory;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.CustomerApprovalAuditMapper;
import com.dtech.admin.mapper.entityToDto.ClaimsApprovalEntityToDto;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.ClaimApprovalService;
import com.dtech.admin.service.EmailNotificationService;
import com.dtech.admin.service.LoginService;
import com.dtech.admin.service.MessageService;
import com.dtech.admin.specifications.ClaimsApprovalSpecification;
import com.dtech.admin.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class ClaimApprovalServiceImpl implements ClaimApprovalService {

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final AuditLogService auditLogService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final WebUserRepository webUserRepository;

    @Autowired
    private final ApprovalWorkFlowRepository approvalWorkFlowRepository;

    @Autowired
    private final CommonPrivilegeGetter commonPrivilegeGetter;

    @Autowired
    private final TreatmentCategoryRepository treatmentCategoryRepository;

    @Autowired
    private final TreatmentRepository treatmentRepository;

    @Autowired
    private final ClaimsApprovalEntityToDto claimsApprovalEntityToDto;

    @Autowired
    private final CustomerApprovalAuditMapper customerApprovalAuditMapper;

    @Autowired
    private final CommonParameterRepository commonParameterRepository;

    @Autowired
    private final InsurancePolicyRepository insurancePolicyRepository;

    @Autowired
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final RemarkRepository remarkRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final InsuranceStaffCategoryPeriodRepository insuranceStaffCategoryPeriodRepository;

    @Autowired
    private final MessageService messageService;

    @Autowired
    private final EmailNotificationService emailNotificationService;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final ObjectMapper objectMapper;

    private static final BigDecimal NS_MAX_CLAIM_AMOUNT = BigDecimal.valueOf(800000);
    private static final int NS_MAX_EMPLOYEE_REQUESTS = 4;

    @Autowired
    private final InsuranceDetailsLimitRepository insuranceDetailsLimitRepository;

    @Autowired
    private final InsuranceQuarterRepository insuranceQuarterRepository;


    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Approval claims request {} ", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.CAPM.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Workflow.values())
                    .filter(status -> !Status.ACTIVE.name().equals(status.name()))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> treatmentCategory = treatmentCategoryRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> remarks = remarkRepository.findAllByRemarkCategoryAndStatus(RemarkCategory.INSURANCE, Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> treatment = treatmentRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getTreatmentCode(), val.getTreatmentDescription())).toList();

            List<SimpleBaseDTO> relationCategory = Arrays.stream(RelationCategory.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> staffCategory = staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> period = insuranceStaffCategoryPeriodRepository.findAll().stream().map(val ->
                    new SimpleBaseDTO(String.valueOf(val.getId()), val.getFromDate().toString() + " to " + val.getToDate().toString() + " " + val.getStaffCategories().getDescription())).toList();

            WebUser webUser = webUserRepository.findByUsername(channelRequestDTO.getUsername()).orElse(null);

            List<SimpleBaseDTO> companyTypes = companyTypeRepository.findAllByStatus(Status.ACTIVE).stream().map(
                    val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            Optional.ofNullable(webUser)
                    .map(WebUser::getApprovalLevel)
                    .ifPresent(approvalLevel -> {
                        switch (approvalLevel) {
                            case LEVEL01 -> privileges.setApprovalL1(true);
                            case LEVEL02 -> privileges.setApprovalL2(true);
                            case LEVEL03 -> privileges.setApprovalL3(true);
                        }
                    });

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            responseMap.put("treatmentCategory", treatmentCategory);
            responseMap.put("treatment", treatment);
            responseMap.put("relationCategory", relationCategory);
            responseMap.put("remarks", remarks);
            responseMap.put("staffCategory", staffCategory);
            responseMap.put("period", period);
            responseMap.put("company", companyTypes);

            auditLogService.log(WebPage.CAPM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.CAPM.name()}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ClaimRequestSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Approval filter list {} ", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            Page<InsuranceClaimsRequest> insuranceClaimsRequests = Objects.nonNull(paginationRequest.getSearch()) ?
                    insuranceClaimsRequestRepository.findAll(ClaimsApprovalSpecification.getSpecification(paginationRequest.getSearch(), false), pageable) :
                    insuranceClaimsRequestRepository.findAll(ClaimsApprovalSpecification.getSpecification(false), pageable);
            log.info("Approval details filter records {}", insuranceClaimsRequests);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    insuranceClaimsRequestRepository.count(ClaimsApprovalSpecification.getSpecification(paginationRequest.getSearch(), false)) :
                    insuranceClaimsRequestRepository.count(ClaimsApprovalSpecification.getSpecification(false));
            log.info("Approval details filter records map start");

            List<ClaimsRequestResponseDTO> responseDTOList = insuranceClaimsRequests.stream()
                    .map(claim -> claimsApprovalEntityToDto.mapClaimsApproval(claim, false)).toList();

            List<Map<String, Object>> sanitizedResponse = responseDTOList.stream()
                    .map(this::sanitizeFilterListResponse)
                    .toList();

            log.info("Approval details filter records map finish");
            List<String> newAuditList = customerApprovalAuditMapper.mapToDTOAudit(insuranceClaimsRequests.stream().toList());
            auditLogService.log(WebPage.CAPM.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<>(sanitizedResponse, sanitizedResponse.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.CLAIMS_DETAILS_DETAILS_FILTER_LIST_SUCCESSFULLY,
                            null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> actionRequest(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Action event for claim request: {}", claimRequestDTO);

            Optional<InsuranceClaimsRequest> optClaim = insuranceClaimsRequestRepository.findById(claimRequestDTO.getId());
            if (optClaim.isEmpty()) {
                log.info("Insurance claim request does not exist: {}", claimRequestDTO.getId());
                return ResponseEntity.ok(responseUtil.error(null, 1044,
                        messageSource.getMessage(ResponseMessageUtil.CLAIMS_REQUEST_NOT_FOUND, new Object[]{claimRequestDTO.getId()}, locale)));
            }

            InsuranceClaimsRequest claim = optClaim.get();

            Optional<WebUser> optUser = webUserRepository.findByUsername(claimRequestDTO.getUsername());
            if (optUser.isEmpty()) {
                log.info("User doesn't exist: {}", claimRequestDTO.getUsername());
                return ResponseEntity.ok(responseUtil.error(null, 1011,
                        messageSource.getMessage(ResponseMessageUtil.SYSTEM_USER_NOT_FOUND_OR_INACTIVE, new Object[]{claimRequestDTO.getUsername()}, locale)));
            }

            if (claimRequestDTO.getStatus().equals(Workflow.APPROVED.name())) {

                if (claim.getRequestAmount().compareTo(claimRequestDTO.getApprovedAmount()) < 0) {
                    log.info("Claim request must be less than or equal to approvedAmount: {}", claimRequestDTO.getApprovedAmount());
                    return ResponseEntity.ok(responseUtil.error(null, 1048,
                            messageSource.getMessage(ResponseMessageUtil.APPROVED_AMOUNT_LESS_THAN_EQUALS_REQUESTED_AMOUNT, null, locale)));
                }

            }

            WebUser user = optUser.get();
            if (user.getApprovalLevel() == null) {
                log.info("User approval level not found: {}", user.getUsername());
                return ResponseEntity.ok(responseUtil.error(null, 1045,
                        messageSource.getMessage(ResponseMessageUtil.CANNOT_PERFORM_ACTION_USER_NOT_ELIGIBLE, null, locale)));
            }

            ApprovalLevel userApprovalLevel = user.getApprovalLevel();
            ApprovalLevel currentApprovalLevel = claim.getApprovalLevel();

            if (!userApprovalLevel.equals(currentApprovalLevel)) {
                log.info("User approval level ({}) does not match claim approval level ({})", userApprovalLevel, currentApprovalLevel);
                return ResponseEntity.ok(responseUtil.error(null, 1046,
                        messageSource.getMessage(ResponseMessageUtil.CANNOT_PERFORM_ACTION_USER_NOT_ELIGIBLE, null, locale)));
            }

            InsuranceStaffCategoryPeriod insuranceStaffCategoryPeriod = null;
            Optional<InsuranceDetailsLimit> byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment = null;

            if (claimRequestDTO.getStatus().equals(Workflow.APPROVED.name())) {
                insuranceStaffCategoryPeriod = insuranceStaffCategoryPeriodRepository.findById(claimRequestDTO.getPolicyId()).orElse(null);

                if (insuranceStaffCategoryPeriod == null) {
                    log.info("Insurance period not found");
                    return ResponseEntity.ok(responseUtil.error(null, 1046,
                            messageSource.getMessage(ResponseMessageUtil.INSURANCE_PERIOD_NOT_FOUND, null, locale)));
                }

                byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment = insuranceDetailsLimitRepository.findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment(
                        claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                        Status.ACTIVE, insuranceStaffCategoryPeriod, claim.getInsuranceClaimsDetails().getTreatment());
            }

            if (claimRequestDTO.getStatus().equals(Workflow.APPROVED.name())) {
                if (claimRequestDTO.getAvailableLimit().compareTo(claimRequestDTO.getApprovedAmount()) < 0) {
                    log.info("Available amount invalid");
                    return ResponseEntity.ok(responseUtil.error(null, 1046,
                            messageSource.getMessage(ResponseMessageUtil.APPROVED_AMOUNT_LESS_THAN_EQUALS_REQUESTED_AMOUNT, null, locale)));
                }
                ResponseEntity<ApiResponse<Object>> validation = claimsApprovalValidation(claim.getEmployee(), claimRequestDTO, claim, insuranceStaffCategoryPeriod, locale);
                if (validation != null) return validation;
            }

            Optional<ApprovalWorkFlow> optionalWorkFlow = claim.getApprovalWorkFlows().stream()
                    .filter(wf -> wf.getApprovalLevel().equals(currentApprovalLevel))
                    .findFirst();

            if (optionalWorkFlow.isEmpty()) {
                log.warn("No approval workflow found for current level: {}", currentApprovalLevel);
                return ResponseEntity.ok(responseUtil.error(null, 1050, "Approval workflow not found."));
            }

            ApprovalWorkFlow workFlow = optionalWorkFlow.get();

            if (!workFlow.getStatus().equals(Workflow.UNDER_REVIEW)) {
                log.info("Workflow already processed: {}", workFlow.getStatus());
                return ResponseEntity.ok(responseUtil.error(null, 1047,
                        messageSource.getMessage(ResponseMessageUtil.ALREADY_PERFORM_ACTION, null, locale)));
            }

            Workflow newStatus = Workflow.valueOf(claimRequestDTO.getStatus());
            workFlow.setStatus(newStatus);
            if (newStatus.equals(Workflow.APPROVED)) {
                workFlow.setApprovedAmount(claimRequestDTO.getApprovedAmount());
            }
            workFlow.setApprovedDate(DateTimeUtil.getCurrentDateTime());
            workFlow.setApprovedUser(claimRequestDTO.getUsername());
            workFlow.setRejectedRemark(claimRequestDTO.getRemark());

            approvalWorkFlowRepository.saveAndFlush(workFlow);

            switch (currentApprovalLevel) {
                case LEVEL01 -> {
                    ApprovalWorkFlow level2Workflow = new ApprovalWorkFlow();
                    level2Workflow.setApprovalLevel(ApprovalLevel.LEVEL02);
                    level2Workflow.setStatus(Workflow.UNDER_REVIEW);
                    approvalWorkFlowRepository.saveAndFlush(level2Workflow);
                    claim.getApprovalWorkFlows().add(level2Workflow);
                    claim.setApprovalLevel(ApprovalLevel.LEVEL02);
                    if (claimRequestDTO.getStatus().equals(Workflow.APPROVED.name())) {
                        claim.setInsuranceDetailsLimit(byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment.get());
                        notifyLevelTwoApprovers(claim, locale);
                    } else if (claimRequestDTO.getStatus().equals(Workflow.REJECTED.name())) {
                        // Notify Level 02 about the rejection with full details.
                        List<WebUser> levelTwoApprovers = webUserRepository.findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL02, Status.ACTIVE);
                        emailNotificationService.notifyLevelTwoOnLevelOneRejection(levelTwoApprovers, claim, claimRequestDTO.getRemark(), locale);
                        claim.setRequestStatus(Workflow.UNDER_REVIEW);
                    }

                }

                case LEVEL02 -> {

                    Workflow status1 = claim.getApprovalWorkFlows().stream()
                            .filter(wf -> wf.getApprovalLevel().equals(ApprovalLevel.LEVEL01))
                            .map(ApprovalWorkFlow::getStatus)
                            .findFirst().orElse(null);

                    BigDecimal appAmount1 = null;

                    if (Workflow.APPROVED.equals(status1)) {

                        appAmount1 = claim.getApprovalWorkFlows().stream()
                                .filter(wf -> wf.getApprovalLevel().equals(ApprovalLevel.LEVEL01))
                                .map(ApprovalWorkFlow::getApprovedAmount)
                                .findFirst().orElse(null);
                    }

                    Workflow status2 = workFlow.getStatus();
                    BigDecimal appAmount2 = workFlow.getApprovedAmount();
                    boolean levelTwoRejected = Workflow.REJECTED.equals(status2);

                    if (status1 != null && !status1.equals(status2) || (appAmount1 != null && appAmount1.compareTo(appAmount2) != 0)) {
                        ApprovalWorkFlow level3Workflow = new ApprovalWorkFlow();
                        level3Workflow.setApprovalLevel(ApprovalLevel.LEVEL03);
                        level3Workflow.setStatus(Workflow.UNDER_REVIEW);
                        approvalWorkFlowRepository.saveAndFlush(level3Workflow);
                        claim.getApprovalWorkFlows().add(level3Workflow);
                        claim.setApprovalLevel(ApprovalLevel.LEVEL03);
                        if (claimRequestDTO.getStatus().equals(Workflow.APPROVED.name())) {
                            claim.setInsuranceDetailsLimit(byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment.get());
                            // Notify Level 03 approvers to take action.
                            List<WebUser> levelThreeApprovers = webUserRepository.findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL03, Status.ACTIVE);
                            emailNotificationService.notifyLevelThreePendingApproval(levelThreeApprovers, claim, locale);
                        }
                    } else {
                        claim.setRequestStatus(status2);
                        claim.setApprovedAmount(workFlow.getApprovedAmount());

                        MessageType messageType = MessageType.INSURANCE_REJECTED;
                        String otherMark = workFlow.getRejectedRemark() != null ? workFlow.getRejectedRemark() : "";

                        if (claim.getRequestStatus().equals(Workflow.APPROVED)) {
                            messageType = MessageType.INSURANCE_APPROVAL;
                            otherMark = buildApprovedAmountMessage(claim.getRequestAmount(), claim.getApprovedAmount(), workFlow.getRejectedRemark());
                            claim.setInsuranceDetailsLimit(byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment.get());

                            List<WebUser> levelOneApprovers = webUserRepository.findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL01, Status.ACTIVE);
                            emailNotificationService.notifyLevelOneOnApproval(levelOneApprovers, claim, workFlow.getApprovedAmount(), ApprovalLevel.LEVEL02, locale);
                        }

                        notifyMessage(claim.getEmployee().getPrimaryMobile(), claim.getRequestId(), messageType, otherMark);

                    }

                    if (levelTwoRejected) {
                        if (Workflow.REJECTED.equals(status1)) {
                            // Level 01 had already rejected; notify Level 01 only with details.
                            List<WebUser> levelOneApprovers = webUserRepository.findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL01, Status.ACTIVE);
                            emailNotificationService.notifyLevelOneOnLevelTwoRejection(levelOneApprovers, claim, workFlow.getRejectedRemark(), locale);
                        } else {
                            // Level 01 approved; escalate rejection details to Level 03 only.
                            List<WebUser> levelThreeApprovers = webUserRepository.findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL03, Status.ACTIVE);
                            emailNotificationService.notifyLevelTwoRejection(levelThreeApprovers, claim, workFlow.getRejectedRemark(), locale);
                        }
                    }
                }

                case LEVEL03 -> {
                    claim.setRequestStatus(newStatus);
                    claim.setApprovedAmount(workFlow.getApprovedAmount());

                    MessageType messageType = MessageType.INSURANCE_REJECTED;
                    String otherMark = workFlow.getRejectedRemark() != null ? workFlow.getRejectedRemark() : "";

                    if (claim.getRequestStatus().equals(Workflow.APPROVED)) {
                        claim.setInsuranceDetailsLimit(byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment.get());
                        messageType = MessageType.INSURANCE_APPROVAL;
                        otherMark = buildApprovedAmountMessage(claim.getRequestAmount(), claim.getApprovedAmount(), workFlow.getRejectedRemark());
                    }

                    List<WebUser> levelOneApprovers = webUserRepository.findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL01, Status.ACTIVE);
                    emailNotificationService.notifyLevelOneFinalDecision(levelOneApprovers, claim, newStatus, workFlow.getRejectedRemark(), locale);
                    notifyMessage(claim.getEmployee().getPrimaryMobile(), claim.getRequestId(), messageType, otherMark);
                }
            }

            insuranceClaimsRequestRepository.saveAndFlush(claim);

            return ResponseEntity.ok(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIMS_APPROVED_SUCCESS, null, locale)));

        } catch (Exception e) {
            log.error("Exception in actionRequest: ", e);
            throw e;
        }
    }

    private Map<String, Object> sanitizeFilterListResponse(ClaimsRequestResponseDTO claimsRequestResponseDTO) {
        Map<String, Object> dtoMap = objectMapper.convertValue(claimsRequestResponseDTO, new TypeReference<Map<String, Object>>() {});

        Object insuranceDetailsObj = dtoMap.get("insuranceClaimsDetails");
        if (insuranceDetailsObj instanceof Map<?, ?> insuranceDetailsMap) {
            ((Map<String, Object>) insuranceDetailsMap).remove("documents");
        }

        Object employeeObj = dtoMap.get("employee");
        if (employeeObj instanceof Map<?, ?> employeeMap) {
            Object personalDetailsObj = ((Map<?, ?>) employeeMap).get("userPersonalDetails");
            if (personalDetailsObj instanceof Map<?, ?> personalDetailsMap) {
                ((Map<String, Object>) personalDetailsMap).remove("birthImg");
            }
        }

        return dtoMap;
    }

    protected void notifyLevelTwoApprovers(InsuranceClaimsRequest claim, Locale locale) {
        try {
            List<WebUser> approvers = webUserRepository.findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL02, Status.ACTIVE);
            if (approvers.isEmpty()) {
                log.warn("No Level 02 approvers available to notify for claim {}", claim.getRequestId());
                return;
            }
            emailNotificationService.notifyLevelTwoPendingApproval(approvers, claim, locale);
        } catch (Exception ex) {
            log.error("Failed to notify Level 02 approvers for claim {}", claim.getRequestId(), ex);
        }
    }

    @Async
    protected void notifyMessage(String mobile, String requestId, MessageType messageType, String otherMark) {
        try {
            messageService.sendMessage(messageType, requestId, otherMark, mobile);
            log.info("Sent OTP, waiting for response... sent password reset");
        } catch (RuntimeException e) {
            log.error(e);
            throw e;
        }
    }

    private String buildApprovedAmountMessage(BigDecimal requestAmount, BigDecimal approvedAmount, String remark) {
        String amountText = String.valueOf(approvedAmount != null ? approvedAmount : BigDecimal.ZERO);

        if (remark != null && !remark.trim().isEmpty()) {
            return amountText + " (Remark: " + remark.trim() + ")";
        }

        return amountText;
    }

    private ResponseEntity<ApiResponse<Object>> claimsApprovalValidation(ApplicationUser user,
                                                                         ClaimRequestDTO claimRequestDTO,
                                                                         InsuranceClaimsRequest insuranceClaimsRequest,
                                                                         InsuranceStaffCategoryPeriod insuranceStaffCategoryPeriod,
                                                                         Locale locale) {
        try {
            log.info("Claim approval validation started");

            if (user.getUserPersonalDetails().getIsTemp()
                    || user.getUserPersonalDetails().getUserCompanyDetails().getFacility().equals(Facility.DEATH)) {
                log.info("Claims request user not eligible {}", user.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1029,
                        messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
            }

            if (user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy() == null) {
                log.info("User not eligible to claim request {}", user.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1029,
                        messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
            }

            if (insuranceClaimsRequest.getClaimsDependents() != null
                    && insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode().equals(TreatmentType.CRIC.name())) {
                log.info("This CRIC facility can't be eligible for dependent claims");
                return ResponseEntity.ok().body(responseUtil.error(null, 1049,
                        messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
            }

            Optional<CommonParameter> paramOpt = commonParameterRepository.findByCode(CommonParam.INSURANCE_CLAIM_REQUEST_PERIOD.name());
            if (paramOpt.isEmpty()) {
                log.info("Common parameter not found for claim period");
                return ResponseEntity.ok().body(responseUtil.error(null, 1036,
                        messageSource.getMessage(ResponseMessageUtil.COMMON_PARAM_NOT_FOUND, null, locale)));
            }

            CommonParameter param = paramOpt.get();
            Date minuesDate = DateTimeUtil.getMinusDate(param.getValue() + 1, insuranceClaimsRequest.getCreatedDate());

            if (insuranceClaimsRequest.getInsuranceClaimsDetails().getToTreatmentDate().before(minuesDate)) {
                log.info("Claim request is older than allowed {}", user.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1037,
                        messageSource.getMessage(ResponseMessageUtil.OLDER_DATE_INSURANCE_CLAIM_REQUEST, null, locale)));
            }

            Long policyId = user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getId();
            Optional<InsurancePolicy> policyOpt = insurancePolicyRepository.findByIdAndStatus(policyId, Status.ACTIVE);
            if (policyOpt.isEmpty()) {
                log.info("Insurance policy not found for user {}", user.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1035,
                        messageSource.getMessage(ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND, null, locale)));
            }

            Optional<ClaimsDependents> claimsDependentsOpt = Optional.empty();
            if (insuranceClaimsRequest.getClaimsDependents() != null) {
                claimsDependentsOpt = claimDependentsRepository.findByIdAndApplicationUserAndStatusAndEligibleFacilityIn(
                        insuranceClaimsRequest.getClaimsDependents().getId(), user, Workflow.APPROVED,
                        List.of(Facility.INSURANCE, Facility.BOTH));

                if (claimsDependentsOpt.isEmpty()) {
                    log.info("Claim dependent not found or not eligible for insurance");
                    return ResponseEntity.ok().body(responseUtil.error(null, 1034,
                            messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_NOT_FOUND_OR_FACILITY_NOT_ELIGIBLE, null, locale)));
                }

                boolean isDeathClaimExists = deathClaimRequestRepository.existsByClaimsDependentsAndEmployeeAndRequestStatusIn(
                        claimsDependentsOpt.get(), user, List.of(Workflow.APPROVED));
                if (isDeathClaimExists) {
                    log.info("Claim dependent death claim request already approved");
                    return ResponseEntity.ok().body(responseUtil.error(null, 1047,
                            messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_DEATH_REQUEST_ALREADY_PROCEED, null, locale)));
                }
            }

            BigDecimal sumOfClaims = insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                    user,
                    insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode(),
                    insuranceStaffCategoryPeriod.getId(),
                    List.of(Workflow.APPROVED)
            );

            String staffCategoryCode = user.getUserPersonalDetails()
                    .getUserCompanyDetails()
                    .getStaffCategories()
                    .getCode();

            boolean isCRIC = insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode().equals(TreatmentType.CRIC.name());

            sumOfClaims = sumOfClaims != null ? sumOfClaims : BigDecimal.ZERO;

            InsuranceDetailsLimit insuranceDetailsLimit = insuranceClaimsRequest.getInsuranceDetailsLimit();
            InsuranceQuarter insuranceQuarter = insuranceClaimsRequest.getInsuranceQuarter();

            BigDecimal requestAmount = claimRequestDTO.getApprovedAmount();

            if (insuranceDetailsLimit.getIsQuarter()) {
                BigDecimal sumOfClaimsCategory = insuranceClaimsRequestRepository
                        .getSumApprovedAmountByEmployeeAndTreatmentAndTreatmentCategoryAndPeriod(
                                user,
                                insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode(),
                                insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory().getCode(),
                                insuranceStaffCategoryPeriod.getId(),
                                List.of(Workflow.APPROVED)
                        );

                sumOfClaimsCategory = sumOfClaimsCategory != null ? sumOfClaimsCategory : BigDecimal.ZERO;

                BigDecimal fundLimit = BigDecimal.ZERO;
                if (insuranceClaimsRequest.getInsuranceQuarter() != null) {
                    fundLimit = insuranceQuarter.getQuarterLimit();
                } else {

                    fundLimit = insuranceDetailsLimit.getGlobalLimit();
                }

                BigDecimal remainingBalance = fundLimit.subtract(sumOfClaimsCategory);
                log.info("Quarter limit check -> treatment {}, category {}, period {}, sumApproved {}, fundLimit {}, remaining {}",
                        insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode(),
                        insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory().getCode(),
                        insuranceStaffCategoryPeriod.getId(),
                        sumOfClaimsCategory, fundLimit, remainingBalance);

                if (requestAmount.compareTo(remainingBalance) > 0) {
                    log.info("Request fund limit exceeded with quarter limit. Used: {}, Fund limit: {}, Requested: {}, Remaining: {}",
                            sumOfClaimsCategory, fundLimit, requestAmount, remainingBalance);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1052,
                            messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{remainingBalance}, locale)));
                }

                if (isCRIC && "NS".equals(staffCategoryCode)) {
                    log.info("Dependent eligible due to CRIC {} ", sumOfClaims);
                    int requestEmp = insuranceClaimsRequestRepository.
                            countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndRequestStatusIn(TreatmentType.CRIC.name(), List.of(Workflow.APPROVED));
                    log.info("Dependent not eligible due to CRIC {} {}", sumOfClaims, requestEmp);

                    boolean exists = insuranceClaimsRequestRepository.
                            existsByEmployeeAndInsuranceClaimsDetails_Treatment_TreatmentCodeAndRequestStatus(user,
                                    TreatmentType.CRIC.name(),
                                    Workflow.APPROVED);

                    if (requestEmp > NS_MAX_EMPLOYEE_REQUESTS
                            || exists
                            || insuranceClaimsRequest.getRequestAmount().compareTo(NS_MAX_CLAIM_AMOUNT) > 0) {

                        return ResponseEntity.ok().body(
                                responseUtil.error(
                                        null,
                                        1034,
                                        messageSource.getMessage(
                                                ResponseMessageUtil.NORMAL_STAFF_CLAIM_LIMIT_OR_OUT_OF_EMPLOYEE_REQUEST_EXCEED,
                                                null,
                                                locale
                                        )
                                )
                        );
                    }
                    insuranceDetailsLimit.setGlobalLimit(NS_MAX_CLAIM_AMOUNT);
                    sumOfClaims = null;
                } else if (isCRIC && "SNR".equals(staffCategoryCode)) {
                    log.info("Dependent eligible due to CRIC {} ", sumOfClaims);
                    int requestEmp = insuranceClaimsRequestRepository.
                            countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndRequestStatusInAndEmployee(TreatmentType.CRIC.name(), List.of(Workflow.APPROVED), user);
                    log.info("Dependent not eligible due to CRIC {} {}", sumOfClaims, requestEmp);

                    if (requestEmp > 4 || sumOfClaims.compareTo(BigDecimal.valueOf(2000000)) > 0 || insuranceClaimsRequest.getRequestAmount().compareTo(BigDecimal.valueOf(500000)) > 0) {
                        log.info("Invalid claim limit {} {} ", sumOfClaims, requestEmp);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1034, messageSource.getMessage(ResponseMessageUtil.NORMAL_STAFF_CLAIM_LIMIT_OR_OUT_OF_EMPLOYEE_REQUEST_EXCEED, null, locale)));

                    }
                }

            } else {
                if (insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory().getCode().equals(TreatmentCategory.OTHER.name())) {
                    BigDecimal fundLimit = insuranceDetailsLimit.getGlobalLimit();
                    BigDecimal remainingBalance = fundLimit.subtract(sumOfClaims);

                    if (requestAmount.compareTo(remainingBalance) > 0) {
                        log.info("Request fund limit exceeded for OTHER treatment");
                        return ResponseEntity.ok().body(responseUtil.error(null, 1052,
                                messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{remainingBalance}, locale)));
                    }

                } else {
                    BigDecimal sumOfClaimsCategory = insuranceClaimsRequestRepository
                            .getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
                                    user,
                                    insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode(),
                                    insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory().getCode(),
                                    insuranceStaffCategoryPeriod.getId(),
                                    List.of(Workflow.APPROVED));

                    sumOfClaimsCategory = sumOfClaimsCategory != null ? sumOfClaimsCategory : BigDecimal.ZERO;

                    BigDecimal fundLimit = insuranceQuarter.getQuarterLimit();

                    if (sumOfClaimsCategory.compareTo(fundLimit) >= 0
                            || fundLimit.subtract(sumOfClaimsCategory).compareTo(requestAmount) < 0) {
                        log.info("Request exceeds treatment category limit");
                        return ResponseEntity.ok().body(responseUtil.error(null, 1052,
                                messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{fundLimit}, locale)));
                    }

                    BigDecimal remainingBalance = insuranceDetailsLimit.getGlobalLimit().subtract(sumOfClaims);
                    if (requestAmount.compareTo(remainingBalance) > 0) {
                        log.info("Request exceeds global limit");
                        return ResponseEntity.ok().body(responseUtil.error(null, 1052,
                                messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{remainingBalance}, locale)));
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> view(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Claims request details view {}", claimRequestDTO);
            return insuranceClaimsRequestRepository.findById(claimRequestDTO.getId()).map(claimsRequest -> {
                ClaimsRequestResponseDTO claimsRequestResponseDTO = claimsApprovalEntityToDto.mapClaimsApproval(claimsRequest, true);

                Date currentDate = DateTimeUtil.getCurrentDateTime();
                String staffCode = claimsRequest.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode();
                Date permanentDate = claimsRequest.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getPermanentDate();

                List<InsuranceStaffCategoryPeriod> policyPeriods = resolvePolicyPeriods(currentDate, permanentDate, staffCode);
                Map<String, Object> limit = new LinkedHashMap<>();
                List<SimpleBaseDTO> policyList = new ArrayList<>();

                InsurancePolicy insurancePolicy = claimsRequest.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy();
                Treatment treatment = claimsRequest.getInsuranceClaimsDetails().getTreatment();

                for (InsuranceStaffCategoryPeriod period : policyPeriods) {
                    SimpleBaseDTO periodDto = new SimpleBaseDTO(String.valueOf(period.getId()),
                            period.getFromDate().toString() + " to " + period.getToDate().toString() + " " + period.getStaffCategories().getDescription());
                    policyList.add(periodDto);

                    Map<String, AvailableInsuranceLimitDTO> periodLimits = new HashMap<>();
                    Optional<InsuranceDetailsLimit> insuranceDetailsLimits = insuranceDetailsLimitRepository
                            .findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment(
                                    insurancePolicy,
                                    Status.ACTIVE,
                                    period,
                                    treatment);
                    if (insuranceDetailsLimits.isPresent()) {
                        try {
                            setLimitMap(periodLimits, insuranceDetailsLimits.get(), claimsRequest.getEmployee(), claimsRequest);
                        } catch (ParseException e) {
                            log.error(e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }

                    limit.put(periodDto.getCode(), periodLimits);
                }

                claimsRequestResponseDTO.setLimits(limit);
                claimsRequestResponseDTO.setPolicyList(policyList);

                List<String> newAuditList = customerApprovalAuditMapper.mapToDTOAudit(List.of(claimsRequest));
                auditLogService.log(WebPage.CAPM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(),
                        claimRequestDTO.getIp(), claimRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, claimRequestDTO.getUsername());

                return ResponseEntity.ok().body(responseUtil.success((Object) claimsRequestResponseDTO,
                        messageSource.getMessage(ResponseMessageUtil.CLAIMS_DETAILS_RETRIEVE_SUCCESSFULLY, null, locale)));
            }).orElseGet(() -> {
                log.info("Claim details not found {}", claimRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1043,
                        messageSource.getMessage(ResponseMessageUtil.CLAIMS_DETAILS_NOT_FOUND, new Object[]{claimRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private List<InsuranceStaffCategoryPeriod> resolvePolicyPeriods(Date currentDate, Date permanentDate, String staffCode) {
        Optional<InsuranceStaffCategoryPeriod> currentPeriodOpt = insuranceStaffCategoryPeriodRepository
                .findByDateWithinRange(currentDate, staffCode)
                .filter(period -> period.getStatus() == Status.ACTIVE);

        if (currentPeriodOpt.isEmpty()) {
            return Collections.emptyList();
        }

        InsuranceStaffCategoryPeriod currentPeriod = currentPeriodOpt.get();
        Optional<InsuranceStaffCategoryPeriod> startPeriodOpt = Optional.empty();

        if (permanentDate != null) {
            startPeriodOpt = insuranceStaffCategoryPeriodRepository
                    .findByDateWithinRangeExclusiveEnd(permanentDate, staffCode)
                    .filter(period -> period.getStatus() == Status.ACTIVE);

            if (startPeriodOpt.isEmpty()) {
                startPeriodOpt = insuranceStaffCategoryPeriodRepository
                        .findFirstByStaffCategories_CodeAndStatusAndFromDateAfterOrderByFromDateAsc(
                                staffCode, Status.ACTIVE, permanentDate);
            }
        }

        Date startDate = startPeriodOpt.map(InsuranceStaffCategoryPeriod::getFromDate)
                .orElse(currentPeriod.getFromDate());

        List<InsuranceStaffCategoryPeriod> periods = insuranceStaffCategoryPeriodRepository
                .findByStaffCategories_CodeAndStatusAndFromDateGreaterThanEqualAndFromDateLessThanEqualOrderByFromDateAsc(
                        staffCode, Status.ACTIVE, startDate, currentPeriod.getFromDate());

        if (periods.isEmpty()) {
            return List.of(currentPeriod);
        }

        return periods;
    }

    @Transactional(readOnly = true)
    public void setLimitMap(Map<String, AvailableInsuranceLimitDTO> limitMap,
                            InsuranceDetailsLimit insuranceDetailsLimit,
                            ApplicationUser applicationUser,
                            InsuranceClaimsRequest insuranceClaimsRequest) throws ParseException {

        try {
            log.info("Insurance ref {}", insuranceDetailsLimit.getId());

            Date permentDateTime = applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate();

            InsuranceStaffCategoryPeriod currentPeriod = insuranceDetailsLimit.getInsuranceStaffCategoryPeriod();
            String treatmentCode = insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode();
            Long periodId = currentPeriod != null ? currentPeriod.getId() : null;

            BigDecimal sum = BigDecimal.ZERO;
            if (periodId != null) {
                BigDecimal currentSum = insuranceClaimsRequestRepository
                        .getSumApprovedAmountByEmployeeAndTreatmentAndPeriod(
                                applicationUser,
                                treatmentCode,
                                periodId,
                                List.of(Workflow.APPROVED));
                if (currentSum != null) {
                    sum = currentSum;
                }
            }

            Date previousPermanentDate = applicationUser.getUserPersonalDetails()
                    .getUserCompanyDetails()
                    .getPreviousPermanentDate();
            Date changeDate = previousPermanentDate != null
                    ? applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate()
                    : null;
            InsuranceStaffCategoryPeriod prevPeriod = null;
            if (changeDate != null
                    && currentPeriod != null
                    && currentPeriod.getStaffCategories() != null) {
                prevPeriod = insuranceStaffCategoryPeriodRepository
                        .findByDateWithinRangeAnyStaff(changeDate)
                        .stream()
                        .filter(p -> p.getStaffCategories() != null)
                        .filter(p -> !p.getStaffCategories().getCode()
                                .equals(currentPeriod.getStaffCategories().getCode()))
                        .findFirst()
                        .orElse(null);
                if (prevPeriod != null) {
                    BigDecimal prevSum = insuranceClaimsRequestRepository
                            .getSumApprovedAmountByEmployeeAndTreatmentAndPeriod(
                                    applicationUser,
                                    treatmentCode,
                                    prevPeriod.getId(),
                                    List.of(Workflow.APPROVED));
                    if (prevSum != null) {
                        sum = sum.add(prevSum);
                    }
                    log.info("CLAIM_APPROVAL_LIMIT carryOver prevPeriodId={}, prevSum={}",
                            prevPeriod.getId(), prevSum);
                }
            }
            log.info("CLAIM_APPROVAL_LIMIT periodId={}, sumCurrent={}", periodId, sum);

            List<InsuranceQuarter> quarters = insuranceDetailsLimit.getInsuranceQuarters();
            InsuranceQuarter referenceQuarter = selectQuarterByPermanentDate(quarters, permentDateTime);
            Date rangeFrom = referenceQuarter != null ? referenceQuarter.getFromDate() : null;
            Date rangeTo = referenceQuarter != null ? referenceQuarter.getToDate() : null;

            Map<String, List<InsuranceQuarter>> byCategory = quarters == null ? Map.of() : quarters.stream()
                    .filter(q -> q.getTreatmentCategory() != null)
                    .collect(java.util.stream.Collectors.groupingBy(q -> q.getTreatmentCategory().getCode()));

            String claimCategoryCode = insuranceClaimsRequest.getInsuranceClaimsDetails() != null
                    && insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory() != null
                    ? insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory().getCode()
                    : null;
            BigDecimal categoryFundLimit = null;
            if (claimCategoryCode != null && byCategory.containsKey(claimCategoryCode)) {
                List<InsuranceQuarter> categoryQuarters = byCategory.get(claimCategoryCode);
                InsuranceQuarter categoryQuarter = matchQuarterRange(categoryQuarters, rangeFrom, rangeTo);
                if (categoryQuarter == null && categoryQuarters != null && !categoryQuarters.isEmpty()) {
                    categoryQuarter = categoryQuarters.get(0);
                }
                if (categoryQuarter != null) {
                    categoryFundLimit = categoryQuarter.getQuarterLimit() != null
                            ? categoryQuarter.getQuarterLimit()
                            : insuranceDetailsLimit.getGlobalLimit();
                }
            }

            BigDecimal maxFundLimit = byCategory.values().stream()
                    .map(list -> {
                        InsuranceQuarter categoryQuarter = matchQuarterRange(list, rangeFrom, rangeTo);
                        if (categoryQuarter == null && !list.isEmpty()) {
                            categoryQuarter = list.get(0);
                        }
                        if (categoryQuarter == null) {
                            return null;
                        }
                        return categoryQuarter.getQuarterLimit() != null
                                ? categoryQuarter.getQuarterLimit()
                                : insuranceDetailsLimit.getGlobalLimit();
                    })
                    .filter(java.util.Objects::nonNull)
                    .max(java.util.Comparator.naturalOrder())
                    .orElse(insuranceDetailsLimit.getGlobalLimit());

            BigDecimal treatmentRemaining = (maxFundLimit != null ? maxFundLimit : BigDecimal.ZERO)
                    .subtract(sum);
            if (treatmentRemaining.compareTo(BigDecimal.ZERO) < 0) {
                treatmentRemaining = BigDecimal.ZERO;
            }

            BigDecimal availableLimit = categoryFundLimit != null ? categoryFundLimit : maxFundLimit;
            if (availableLimit == null) {
                availableLimit = BigDecimal.ZERO;
            }
            if (availableLimit.compareTo(treatmentRemaining) > 0) {
                availableLimit = treatmentRemaining;
            }

            AvailableInsuranceLimitDTO dto = limitMap.get("limit");
            if (dto == null) {
                dto = new AvailableInsuranceLimitDTO();
            }
            dto.setFundLimit(categoryFundLimit != null ? categoryFundLimit
                    : (maxFundLimit != null ? maxFundLimit : BigDecimal.ZERO));
            dto.setAvailableLimit(availableLimit);
            limitMap.put("limit", dto);

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private InsuranceQuarter selectQuarterByPermanentDate(List<InsuranceQuarter> quarters, Date permanentDate) {
        if (quarters == null || quarters.isEmpty()) {
            return null;
        }
        List<InsuranceQuarter> sorted = quarters.stream()
                .sorted(java.util.Comparator.comparing(InsuranceQuarter::getFromDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
        InsuranceQuarter first = sorted.get(0);
        if (permanentDate == null || first.getFromDate() == null || first.getToDate() == null) {
            return first;
        }
        if (permanentDate.before(first.getFromDate())) {
            return first;
        }
        for (InsuranceQuarter quarter : sorted) {
            if (quarter.getFromDate() == null || quarter.getToDate() == null) {
                continue;
            }
            if (!permanentDate.before(quarter.getFromDate()) && !permanentDate.after(quarter.getToDate())) {
                return quarter;
            }
        }
        return sorted.get(sorted.size() - 1);
    }

    private InsuranceQuarter matchQuarterRange(List<InsuranceQuarter> quarters, Date rangeFrom, Date rangeTo) {
        if (quarters == null || quarters.isEmpty() || rangeFrom == null || rangeTo == null) {
            return null;
        }
        return quarters.stream()
                .filter(q -> q.getFromDate() != null && q.getToDate() != null)
                .filter(q -> q.getFromDate().equals(rangeFrom) && q.getToDate().equals(rangeTo))
                .findFirst()
                .orElse(null);
    }

    private void addIfNotPresent(List<SimpleBaseDTO> tCategoryList, InsuranceDetailsLimit insuranceDetailsLimit) {
        boolean alreadyPresent = tCategoryList.stream()
                .anyMatch(dto -> dto.getCode().equals(insuranceDetailsLimit.getTreatment().getTreatmentCode()));
        if (!alreadyPresent) {
            tCategoryList.add(new SimpleBaseDTO(insuranceDetailsLimit.getTreatment().getTreatmentCode(),
                    insuranceDetailsLimit.getTreatment().getTreatmentDescription()));
        }
    }

    private void addIfNotPresentTreatmentCategory(List<SimpleBaseDTO> tCategoryList, InsuranceDetailsLimit insuranceDetailsLimit) {

        for (InsuranceQuarter insuranceQuarter : insuranceDetailsLimit.getInsuranceQuarters()) {
            String categoryCode = insuranceQuarter.getTreatmentCategory().getCode();

            boolean alreadyPresent = tCategoryList.stream()
                    .anyMatch(dto -> dto.getCode().equals(categoryCode));

            if (!alreadyPresent) {
                tCategoryList.add(new SimpleBaseDTO(
                        categoryCode,
                        insuranceQuarter.getTreatmentCategory().getDescription()
                ));
            }
        }
    }
}

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
import com.dtech.admin.service.ClaimEmailRecipientService;
import com.dtech.admin.service.CompanyAccessService;
import com.dtech.admin.service.EmailNotificationService;
import com.dtech.admin.service.LoginService;
import com.dtech.admin.service.MessageService;
import com.dtech.admin.specifications.ClaimsApprovalSpecification;
import com.dtech.admin.specifications.CompanyScopeSpecification;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class ClaimApprovalServiceImpl implements ClaimApprovalService {
    private static final int NORMAL_STAFF_PARENT_MAX_CLAIM_AGE = 65;
    private static final int NORMAL_STAFF_PARENT_EXTRA_ELIGIBLE_DAYS = 14;
    private static final int CHILD_MAX_CLAIM_AGE = 25;
    private static final int CHILD_EXTRA_ELIGIBLE_DAYS = 14;
    private static final int NORMAL_STAFF_EMPLOYEE_MAX_CLAIM_AGE = 60;
    private static final int NORMAL_STAFF_EMPLOYEE_EXTRA_ELIGIBLE_DAYS = 14;
    private static final int OTHER_STAFF_EMPLOYEE_MAX_CLAIM_AGE = 70;
    private static final int OTHER_STAFF_EMPLOYEE_EXTRA_ELIGIBLE_DAYS = 14;
    private static final int NORMAL_STAFF_SPOUSE_MAX_CLAIM_AGE = 60;
    private static final int NORMAL_STAFF_SPOUSE_EXTRA_ELIGIBLE_DAYS = 14;
    private static final int OTHER_STAFF_SPOUSE_MAX_CLAIM_AGE = 70;
    private static final int OTHER_STAFF_SPOUSE_EXTRA_ELIGIBLE_DAYS = 14;
    private static final int NORMAL_STAFF_CRIC_MIN_PERMANENT_YEARS = 3;

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
    private final ApprovalWorkflowRejectReasonRepository approvalWorkflowRejectReasonRepository;

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
    private final ClaimEmailRecipientService claimEmailRecipientService;

    @Autowired
    private final CompanyAccessService companyAccessService;

    @Autowired
    private final ObjectMapper objectMapper;

    private static final BigDecimal NS_MAX_CLAIM_AMOUNT = BigDecimal.valueOf(800000);
    private static final int NS_MAX_EMPLOYEE_REQUESTS = 4;

    @Autowired
    private final InsuranceDetailsLimitRepository insuranceDetailsLimitRepository;

    @Autowired
    private final InsuranceQuarterRepository insuranceQuarterRepository;

    @Autowired
    private final RejoinCarryForwardService rejoinCarryForwardService;


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

            List<RemarkReferenceDTO> remarks = remarkRepository
                    .findAllByRemarkCategoryAndStatus(RemarkCategory.INSURANCE, Status.ACTIVE)
                    .stream()
                    .map(val -> new RemarkReferenceDTO(
                            val.getCode(),
                            val.getDescription(),
                            val.isIncludeInRejectedClaimReport()))
                    .toList();

            List<SimpleBaseDTO> treatment = treatmentRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getTreatmentCode(), val.getTreatmentDescription())).toList();

            List<SimpleBaseDTO> relationCategory = Arrays.stream(RelationCategory.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> staffCategory = staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> period = insuranceStaffCategoryPeriodRepository.findAll().stream().map(val ->
                    new SimpleBaseDTO(String.valueOf(val.getId()), val.getFromDate().toString() + " to " + val.getToDate().toString() + " " + val.getStaffCategories().getDescription())).toList();

            WebUser webUser = webUserRepository.findByUsername(channelRequestDTO.getUsername()).orElse(null);

            List<SimpleBaseDTO> companyTypes = companyAccessService.activeCompanies(channelRequestDTO.getUsername()).stream().map(
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

            Specification<InsuranceClaimsRequest> specification = Objects.nonNull(paginationRequest.getSearch())
                    ? ClaimsApprovalSpecification.getSpecification(paginationRequest.getSearch(), false)
                    : ClaimsApprovalSpecification.getSpecification(false);
            specification = specification.and(CompanyScopeSpecification.companyCodeIn(
                    companyAccessService.activeCompanyCodes(paginationRequest.getUsername()),
                    "employee", "userPersonalDetails", "userCompanyDetails", "companyTypes", "code"));
            Page<InsuranceClaimsRequest> insuranceClaimsRequests = insuranceClaimsRequestRepository.findAll(specification, pageable);
            log.info("Approval details filter records {}", insuranceClaimsRequests);
            long totalElements = insuranceClaimsRequestRepository.count(specification);
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

            Optional<InsuranceClaimsRequest> optClaim = insuranceClaimsRequestRepository.findById(claimRequestDTO.getId())
                    .filter(claim -> canAccess(claim, claimRequestDTO.getUsername()));
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

            BigDecimal effectiveApprovedAmount = resolveEffectiveApprovedAmount(claimRequestDTO);
            RejectReasonBuildResult rejectReasonResult = validateAndBuildRejectReasons(claimRequestDTO, claim, effectiveApprovedAmount);
            if (rejectReasonResult.error() != null) {
                return ResponseEntity.ok(responseUtil.error(null, 1048,
                        rejectReasonResult.error()));
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
            Optional<InsuranceDetailsLimit> byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment = Optional.empty();

            if (claimRequestDTO.getPolicyId() != null) {
                insuranceStaffCategoryPeriod = insuranceStaffCategoryPeriodRepository.findById(claimRequestDTO.getPolicyId()).orElse(null);

                if (insuranceStaffCategoryPeriod == null) {
                    log.info("Insurance period not found");
                    return ResponseEntity.ok(responseUtil.error(null, 1046,
                            messageSource.getMessage(ResponseMessageUtil.INSURANCE_PERIOD_NOT_FOUND, null, locale)));
                }
                UserCompanyDetails companyDetails = claim.getEmployee()
                        .getUserPersonalDetails()
                        .getUserCompanyDetails();
                InsurancePolicy selectedInsurancePolicy = resolveInsurancePolicyForPeriod(
                        companyDetails,
                        insuranceStaffCategoryPeriod,
                        resolveClaimPolicyByPeriod(claim.getEmployee()));
                byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment = insuranceDetailsLimitRepository.findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment(
                        selectedInsurancePolicy,
                        Status.ACTIVE, insuranceStaffCategoryPeriod, claim.getInsuranceClaimsDetails().getTreatment());

                if (byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment.isEmpty()) {
                    log.info("Insurance details limit not found for selected period {} and policy {}",
                            claimRequestDTO.getPolicyId(),
                            selectedInsurancePolicy != null ? selectedInsurancePolicy.getCode() : null);
                    return ResponseEntity.ok(responseUtil.error(null, 1046,
                            messageSource.getMessage(ResponseMessageUtil.INSURANCE_PERIOD_NOT_FOUND, null, locale)));
                }
                applySelectedInsuranceDetailsLimit(
                        claim, byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment);
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
                workFlow.setApprovedAmount(effectiveApprovedAmount);
            } else if (newStatus.equals(Workflow.REJECTED)) {
                workFlow.setApprovedAmount(effectiveApprovedAmount);
            }
            workFlow.setApprovedDate(DateTimeUtil.getCurrentDateTime());
            workFlow.setApprovedUser(claimRequestDTO.getUsername());
            replaceRejectReasons(workFlow, rejectReasonResult.reasons());
            String combinedRejectRemark = ApprovalRemarkUtil.resolveWorkflowRemark(workFlow);
            workFlow.setRejectedRemark(hasText(combinedRejectRemark) ? combinedRejectRemark : claimRequestDTO.getRemark());
            if (insuranceStaffCategoryPeriod != null) {
                workFlow.setPolicy(insuranceStaffCategoryPeriod);
            }

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
                        List<WebUser> levelTwoApprovers = claimEmailRecipientService.resolve(
                                ClaimEmailEvent.CLAIM_L1_REJECTED, claim, ApprovalLevel.LEVEL02);
                        scheduleClaimEmailAfterCommit("level 02 info after level 01 rejection", claim, levelTwoApprovers,
                                () -> emailNotificationService.notifyLevelTwoOnLevelOneRejection(levelTwoApprovers, claim, claimRequestDTO.getRemark(), locale));
                        applySelectedInsuranceDetailsLimit(claim, byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment);
                        claim.setRequestStatus(Workflow.UNDER_REVIEW);
                    }

                }

                case LEVEL02 -> {

                    ApprovalWorkFlow levelOneWorkflow = claim.getApprovalWorkFlows().stream()
                            .filter(wf -> wf.getApprovalLevel().equals(ApprovalLevel.LEVEL01))
                            .findFirst().orElse(null);

                    Workflow status1 = levelOneWorkflow != null ? levelOneWorkflow.getStatus() : null;
                    BigDecimal appAmount1 = Workflow.APPROVED.equals(status1)
                            ? levelOneWorkflow.getApprovedAmount()
                            : null;
                    Long policyId1 = levelOneWorkflow != null && levelOneWorkflow.getPolicy() != null
                            ? levelOneWorkflow.getPolicy().getId()
                            : null;

                    Workflow status2 = workFlow.getStatus();
                    BigDecimal appAmount2 = workFlow.getApprovedAmount();
                    Long policyId2 = workFlow.getPolicy() != null ? workFlow.getPolicy().getId() : null;
                    boolean levelTwoRejected = Workflow.REJECTED.equals(status2);
                    boolean policyMismatch = status1 != null
                            && status2 != null
                            && !Workflow.UNDER_REVIEW.equals(status1)
                            && !Workflow.UNDER_REVIEW.equals(status2)
                            && policyId1 != null
                            && policyId2 != null
                            && !policyId1.equals(policyId2);
                    boolean rejectRemarkMismatch = levelOneWorkflow != null
                            && !normalizeApprovalRemark(ApprovalRemarkUtil.resolveWorkflowRemark(levelOneWorkflow))
                            .equals(normalizeApprovalRemark(ApprovalRemarkUtil.resolveWorkflowRemark(workFlow)));
                    if ((status1 != null && !status1.equals(status2))
                            || (appAmount1 != null && appAmount1.compareTo(appAmount2) != 0)
                            || policyMismatch
                            || rejectRemarkMismatch) {
                        ApprovalWorkFlow level3Workflow = new ApprovalWorkFlow();
                        level3Workflow.setApprovalLevel(ApprovalLevel.LEVEL03);
                        level3Workflow.setStatus(Workflow.UNDER_REVIEW);
                        approvalWorkFlowRepository.saveAndFlush(level3Workflow);
                        claim.getApprovalWorkFlows().add(level3Workflow);
                        claim.setApprovalLevel(ApprovalLevel.LEVEL03);
                        if (claimRequestDTO.getStatus().equals(Workflow.APPROVED.name())) {
                            // Notify Level 03 approvers to take action.
                            List<WebUser> levelThreeApprovers = claimEmailRecipientService.resolve(
                                    ClaimEmailEvent.CLAIM_L2_DIFFERENT_DECISION, claim, ApprovalLevel.LEVEL03);
                            scheduleClaimEmailAfterCommit("level 03 escalation", claim, levelThreeApprovers,
                                    () -> emailNotificationService.notifyLevelThreePendingApproval(levelThreeApprovers, claim, locale));
                        } else if (levelTwoRejected && Workflow.REJECTED.equals(status1)) {
                            List<WebUser> levelThreeApprovers = claimEmailRecipientService.resolve(
                                    ClaimEmailEvent.CLAIM_L2_DIFFERENT_DECISION, claim, ApprovalLevel.LEVEL03);
                            scheduleClaimEmailAfterCommit("level 03 escalation", claim, levelThreeApprovers,
                                    () -> emailNotificationService.notifyLevelThreePendingApproval(levelThreeApprovers, claim, locale));
                        }
                    } else {
                        claim.setRequestStatus(status2);
                        claim.setApprovedAmount(workFlow.getApprovedAmount());

                        MessageType messageType = MessageType.INSURANCE_REJECTED;
                        String otherMark = buildRejectedReasonMessage(workFlow);

                        if (claim.getRequestStatus().equals(Workflow.APPROVED)) {
                            messageType = resolveApprovalMessageType(claim);
                            otherMark = buildApprovedAmountMessage(claim.getRequestAmount(), claim.getApprovedAmount(), buildRejectedReasonMessage(workFlow));
                            claim.setInsuranceDetailsLimit(byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment.get());

                            List<WebUser> levelOneApprovers = claimEmailRecipientService.resolve(
                                    ClaimEmailEvent.CLAIM_L2_MATCHED_APPROVAL, claim, ApprovalLevel.LEVEL01);
                            scheduleClaimEmailAfterCommit("level 01 notification", claim, levelOneApprovers,
                                    () -> emailNotificationService.notifyLevelOneOnApproval(levelOneApprovers, claim, workFlow.getApprovedAmount(), ApprovalLevel.LEVEL02, locale));
                        } else {
                            applySelectedInsuranceDetailsLimit(claim, byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment);
                        }

                        notifyMessage(resolveClaimNotificationMobile(claim), claim.getRequestId(), messageType, otherMark);

                    }

                    if (levelTwoRejected) {
                        if (Workflow.REJECTED.equals(status1)) {
                            // Level 01 had already rejected; notify Level 01 only with details.
                            List<WebUser> levelOneApprovers = claimEmailRecipientService.resolve(
                                    ClaimEmailEvent.CLAIM_L2_REJECTED_AFTER_L1_REJECTED, claim, ApprovalLevel.LEVEL01);
                            scheduleClaimEmailAfterCommit("level 01 rejection after level 02", claim, levelOneApprovers,
                                    () -> emailNotificationService.notifyLevelOneOnLevelTwoRejection(levelOneApprovers, claim, workFlow.getRejectedRemark(), locale));
                        } else {
                            // Level 01 approved; escalate rejection details to Level 03 only.
                            List<WebUser> levelThreeApprovers = claimEmailRecipientService.resolve(
                                    ClaimEmailEvent.CLAIM_L2_REJECTED_AFTER_L1_APPROVED, claim, ApprovalLevel.LEVEL03);
                            scheduleClaimEmailAfterCommit("level 02 rejection", claim, levelThreeApprovers,
                                    () -> emailNotificationService.notifyLevelTwoRejection(levelThreeApprovers, claim, workFlow.getRejectedRemark(), locale));
                        }
                    }
                }

                case LEVEL03 -> {
                    claim.setRequestStatus(newStatus);
                    claim.setApprovedAmount(workFlow.getApprovedAmount());

                    MessageType messageType = MessageType.INSURANCE_REJECTED;
                    String otherMark = buildRejectedReasonMessage(workFlow);

                    if (claim.getRequestStatus().equals(Workflow.APPROVED)) {
                        claim.setInsuranceDetailsLimit(byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment.get());
                        messageType = resolveApprovalMessageType(claim);
                        otherMark = buildApprovedAmountMessage(claim.getRequestAmount(), claim.getApprovedAmount(), buildRejectedReasonMessage(workFlow));
                    } else {
                        applySelectedInsuranceDetailsLimit(claim, byInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment);
                    }

                    List<WebUser> levelOneApprovers = claimEmailRecipientService.resolve(
                            ClaimEmailEvent.CLAIM_L3_FINAL_DECISION, claim, ApprovalLevel.LEVEL01);
                    scheduleClaimEmailAfterCommit("level 01 final decision", claim, levelOneApprovers,
                            () -> emailNotificationService.notifyLevelOneFinalDecision(levelOneApprovers, claim, newStatus, workFlow.getRejectedRemark(), locale));
                    notifyMessage(resolveClaimNotificationMobile(claim), claim.getRequestId(), messageType, otherMark);
                }
            }


            insuranceClaimsRequestRepository.saveAndFlush(claim);

            return ResponseEntity.ok(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIMS_APPROVED_SUCCESS, null, locale)));

        } catch (Exception e) {
            log.error("Exception in actionRequest: ", e);
            throw e;
        }
    }

    private Long resolveClaimPolicyPeriodId(InsuranceClaimsRequest claim) {
        return Optional.ofNullable(claim)
                .map(InsuranceClaimsRequest::getInsuranceDetailsLimit)
                .map(InsuranceDetailsLimit::getInsuranceStaffCategoryPeriod)
                .map(InsuranceStaffCategoryPeriod::getId)
                .orElse(null);
    }

    private void applySelectedInsuranceDetailsLimit(InsuranceClaimsRequest claim,
                                                    Optional<InsuranceDetailsLimit> selectedInsuranceDetailsLimit) {
        selectedInsuranceDetailsLimit.ifPresent(claim::setInsuranceDetailsLimit);
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
            List<WebUser> approvers = claimEmailRecipientService.resolve(
                    ClaimEmailEvent.CLAIM_L1_APPROVED, claim, ApprovalLevel.LEVEL02);
            if (approvers.isEmpty()) {
                log.warn("No Level 02 approvers available to notify for claim {}", claim.getRequestId());
                return;
            }
            scheduleClaimEmailAfterCommit("level 02 escalation", claim, approvers,
                    () -> emailNotificationService.notifyLevelTwoPendingApproval(approvers, claim, locale));
        } catch (Exception ex) {
            log.error("Failed to notify Level 02 approvers for claim {}", claim.getRequestId(), ex);
        }
    }

    private void scheduleClaimEmailAfterCommit(String context,
                                               InsuranceClaimsRequest claim,
                                               List<WebUser> recipients,
                                               Runnable emailTask) {
        initializeClaimEmailData(claim, recipients);
        Runnable safeTask = () -> {
            try {
                emailTask.run();
            } catch (Exception e) {
                log.error("Failed to send {} email after claim approval commit for claim {}",
                        context,
                        claim != null ? claim.getRequestId() : null,
                        e);
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            CompletableFuture.runAsync(safeTask);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(safeTask);
            }
        });
    }

    private void initializeClaimEmailData(InsuranceClaimsRequest claim, List<WebUser> recipients) {
        if (claim != null) {
            claim.getRequestId();
            claim.getRequestAmount();
            claim.getApprovedAmount();
            if (claim.getEmployee() != null) {
                claim.getEmployee().getUsername();
                if (claim.getEmployee().getUserPersonalDetails() != null) {
                    claim.getEmployee().getUserPersonalDetails().getFirstName();
                    claim.getEmployee().getUserPersonalDetails().getLastName();
                }
            }
            if (claim.getApprovalWorkFlows() != null) {
                claim.getApprovalWorkFlows().forEach(workflow -> {
                    workflow.getApprovalLevel();
                    workflow.getStatus();
                    workflow.getApprovedAmount();
                    workflow.getRejectedRemark();
                    if (workflow.getRejectReasons() != null) {
                        workflow.getRejectReasons().forEach(reason -> {
                            reason.getReasonCode();
                            reason.getReasonDescription();
                            reason.getAmount();
                            reason.getRemark();
                        });
                    }
                    if (workflow.getPolicy() != null) {
                        workflow.getPolicy().getId();
                    }
                });
            }
        }
        if (recipients != null) {
            recipients.forEach(WebUser::getEmail);
        }
    }

    protected void notifyMessage(String mobile, String requestId, MessageType messageType, String otherMark) {
        if (!hasText(mobile)) {
            log.warn("Skipping claim SMS notification. Mobile not found for claim {}", requestId);
            return;
        }

        Runnable safeTask = () -> {
            try {
                messageService.sendMessage(messageType, requestId, otherMark, mobile);
                log.info("Claim SMS notification sent for claim {}", requestId);
            } catch (Exception e) {
                log.error("Failed to send claim SMS notification for claim {}", requestId, e);
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            CompletableFuture.runAsync(safeTask);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(safeTask);
            }
        });
    }

    private String resolveClaimNotificationMobile(InsuranceClaimsRequest claim) {
        if (claim != null && claim.getAssistedMobileNo() != null && !claim.getAssistedMobileNo().trim().isEmpty()) {
            return claim.getAssistedMobileNo().trim();
        }
        return claim != null && claim.getEmployee() != null ? claim.getEmployee().getPrimaryMobile() : null;
    }

    private String buildApprovedAmountMessage(BigDecimal requestAmount, BigDecimal approvedAmount, String remark) {
        BigDecimal amount = approvedAmount != null ? approvedAmount : BigDecimal.ZERO;
        String amountText = "Rs. " + amount.stripTrailingZeros().toPlainString();

        if (remark != null && !remark.trim().isEmpty()) {
            return amountText + " (Remark: " + remark.trim() + ")";
        }

        return amountText;
    }

    private MessageType resolveApprovalMessageType(InsuranceClaimsRequest claim) {
        if (claim != null
                && claim.getRequestAmount() != null
                && claim.getApprovedAmount() != null
                && claim.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0
                && claim.getApprovedAmount().compareTo(claim.getRequestAmount()) < 0) {
            return MessageType.INSURANCE_PARTIAL_APPROVAL;
        }
        return MessageType.INSURANCE_APPROVAL;
    }

    private String buildRejectedReasonMessage(ApprovalWorkFlow workFlow) {
        String remark = ApprovalRemarkUtil.formatRejectReasonsForNotification(workFlow.getRejectReasons());
        if (!hasText(remark)) {
            remark = workFlow.getRejectedRemark();
        }
        return remark != null ? remark.trim() : "";
    }

    private BigDecimal resolveEffectiveApprovedAmount(ClaimRequestDTO claimRequestDTO) {
        if (Workflow.REJECTED.name().equals(claimRequestDTO.getStatus())) {
            return claimRequestDTO.getApprovedAmount() != null ? claimRequestDTO.getApprovedAmount() : BigDecimal.ZERO;
        }
        return claimRequestDTO.getApprovedAmount();
    }

    private RejectReasonBuildResult validateAndBuildRejectReasons(ClaimRequestDTO request,
                                                                  InsuranceClaimsRequest claim,
                                                                  BigDecimal approvedAmount) {
        BigDecimal requestAmount = claim.getRequestAmount() != null ? claim.getRequestAmount() : BigDecimal.ZERO;
        BigDecimal approved = approvedAmount != null ? approvedAmount : BigDecimal.ZERO;
        BigDecimal rejectedAmount = requestAmount.subtract(approved);

        if (rejectedAmount.compareTo(BigDecimal.ZERO) < 0) {
            return new RejectReasonBuildResult(List.of(), "Approved amount cannot exceed requested amount.");
        }

        boolean hasRejectedPortion = rejectedAmount.compareTo(BigDecimal.ZERO) > 0
                || Workflow.REJECTED.name().equals(request.getStatus());
        List<com.dtech.admin.dto.request.ApprovalRejectReasonDTO> inputReasons =
                request.getRejectReasons() != null ? request.getRejectReasons() : List.of();
        if (!hasRejectedPortion) {
            inputReasons = inputReasons.stream()
                    .filter(input -> !isEmptyZeroRejectReason(input))
                    .toList();
        }

        if (!hasRejectedPortion && inputReasons.isEmpty()) {
            return new RejectReasonBuildResult(List.of(), null);
        }

        if (hasRejectedPortion && inputReasons.isEmpty()) {
            return new RejectReasonBuildResult(List.of(),
                    "Reject reasons are required when claim has rejected amount.");
        }

        List<ApprovalWorkflowRejectReason> reasons = new ArrayList<>();
        BigDecimal reasonTotal = BigDecimal.ZERO;

        for (com.dtech.admin.dto.request.ApprovalRejectReasonDTO input : inputReasons) {
            if (input == null || !hasText(input.getReasonCode())) {
                return new RejectReasonBuildResult(List.of(), "Reject reason code is required.");
            }
            if (input.getAmount() == null || input.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                return new RejectReasonBuildResult(List.of(), "Reject reason amount cannot be negative.");
            }

            String reasonCode = input.getReasonCode().trim();
            boolean isOther = "OTHER".equalsIgnoreCase(reasonCode);
            if (isOther && !hasText(input.getRemark())) {
                return new RejectReasonBuildResult(List.of(), "Other reject reason remark is required.");
            }

            Remark remark = isOther ? null : remarkRepository
                    .findFirstByCodeIgnoreCaseAndRemarkCategoryAndStatus(reasonCode, RemarkCategory.INSURANCE, Status.ACTIVE)
                    .orElse(null);
            if (!isOther && remark == null) {
                return new RejectReasonBuildResult(List.of(), "Invalid reject reason code: " + reasonCode);
            }

            ApprovalWorkflowRejectReason reason = new ApprovalWorkflowRejectReason();
            reason.setReasonCode(reasonCode.toUpperCase(Locale.ROOT));
            reason.setReasonDescription(isOther ? "Other" : remark.getDescription());
            reason.setReasonCategory(hasText(input.getReasonCategory()) ? input.getReasonCategory().trim() : (isOther ? "Other" : RemarkCategory.INSURANCE.name()));
            reason.setAmount(input.getAmount());
            reason.setRemark(hasText(input.getRemark()) ? input.getRemark().trim() : null);
            reasons.add(reason);
            reasonTotal = reasonTotal.add(input.getAmount());
        }

        if (!hasRejectedPortion) {
            if (reasonTotal.compareTo(BigDecimal.ZERO) == 0) {
                return new RejectReasonBuildResult(reasons, null);
            }
            return new RejectReasonBuildResult(List.of(),
                    "Reject reason amounts must be zero when approved amount equals requested amount.");
        }

        if (reasonTotal.compareTo(rejectedAmount) != 0) {
            return new RejectReasonBuildResult(List.of(),
                    "Approved amount + reject reason amounts must equal requested amount.");
        }

        return new RejectReasonBuildResult(reasons, null);
    }

    private void replaceRejectReasons(ApprovalWorkFlow workFlow, List<ApprovalWorkflowRejectReason> rejectReasons) {
        if (workFlow.getRejectReasons() == null) {
            workFlow.setRejectReasons(new ArrayList<>());
        }
        workFlow.getRejectReasons().clear();
        if (rejectReasons == null || rejectReasons.isEmpty()) {
            return;
        }
        rejectReasons.forEach(reason -> {
            reason.setApprovalWorkFlow(workFlow);
            workFlow.getRejectReasons().add(reason);
        });
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeApprovalRemark(String remark) {
        return remark == null ? "" : remark.trim();
    }

    private boolean isEmptyZeroRejectReason(com.dtech.admin.dto.request.ApprovalRejectReasonDTO input) {
        if (input == null) {
            return true;
        }
        boolean noReasonText = !hasText(input.getReasonCode())
                && !hasText(input.getReasonDescription())
                && !hasText(input.getReasonCategory())
                && !hasText(input.getRemark());
        boolean noAmount = input.getAmount() == null || input.getAmount().compareTo(BigDecimal.ZERO) == 0;
        return noReasonText && noAmount;
    }

    private record RejectReasonBuildResult(List<ApprovalWorkflowRejectReason> reasons, String error) {
    }

    private boolean isParentClaimBlockedForMedical(String staffCategoryCode, com.dtech.admin.enums.MaritalStatus maritalStatus) {
        return Set.of("EX-OP1", "EX-OP2", "MM", "SNR").contains(staffCategoryCode)
                || ("NS".equals(staffCategoryCode) && com.dtech.admin.enums.MaritalStatus.MARRIED.equals(maritalStatus));
    }

    private boolean isParentWithinNormalStaffMedicalAgeLimit(ApplicationUser user, ClaimsDependents dependent) {
        if (user == null
                || dependent == null
                || !DependentCategory.PARENTS.equals(dependent.getDependentCategory())
                || user.getUserPersonalDetails() == null
                || user.getUserPersonalDetails().getUserCompanyDetails() == null
                || user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories() == null) {
            return true;
        }
        String staffCategoryCode = user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode();
        if (!"NS".equalsIgnoreCase(staffCategoryCode)
                || com.dtech.admin.enums.MaritalStatus.MARRIED.equals(user.getUserPersonalDetails().getMaritalStatus())) {
            return true;
        }
        return isDateWithinAgeLimit(
                dependent.getDob(),
                NORMAL_STAFF_PARENT_MAX_CLAIM_AGE,
                NORMAL_STAFF_PARENT_EXTRA_ELIGIBLE_DAYS
        );
    }

    private boolean isChildWithinMedicalAgeLimit(ClaimsDependents dependent) {
        if (dependent == null || !DependentCategory.CHILDREN.equals(dependent.getDependentCategory())) {
            return true;
        }
        return isDateWithinAgeLimit(dependent.getDob(), CHILD_MAX_CLAIM_AGE, CHILD_EXTRA_ELIGIBLE_DAYS);
    }

    private boolean isSpouseWithinMedicalAgeLimit(String staffCategoryCode, ClaimsDependents dependent) {
        if (dependent == null || !DependentCategory.SPOUSE.equals(dependent.getDependentCategory())) {
            return true;
        }
        if ("NS".equals(staffCategoryCode)) {
            return isDateWithinAgeLimit(
                    dependent.getDob(),
                    NORMAL_STAFF_SPOUSE_MAX_CLAIM_AGE,
                    NORMAL_STAFF_SPOUSE_EXTRA_ELIGIBLE_DAYS
            );
        }
        return isDateWithinAgeLimit(
                dependent.getDob(),
                OTHER_STAFF_SPOUSE_MAX_CLAIM_AGE,
                OTHER_STAFF_SPOUSE_EXTRA_ELIGIBLE_DAYS
        );
    }

    private int resolveSpouseClaimMaxAge(String staffCategoryCode) {
        return "NS".equals(staffCategoryCode)
                ? NORMAL_STAFF_SPOUSE_MAX_CLAIM_AGE
                : OTHER_STAFF_SPOUSE_MAX_CLAIM_AGE;
    }

    private boolean isNormalStaffEmployeeWithinMedicalAgeLimit(ApplicationUser user) {
        if (user == null || user.getUserPersonalDetails() == null || user.getUserPersonalDetails().getDob() == null) {
            return false;
        }
        return isDateWithinAgeLimit(
                user.getUserPersonalDetails().getDob(),
                NORMAL_STAFF_EMPLOYEE_MAX_CLAIM_AGE,
                NORMAL_STAFF_EMPLOYEE_EXTRA_ELIGIBLE_DAYS
        );
    }

    private boolean isOtherStaffEmployeeWithinMedicalAgeLimit(ApplicationUser user) {
        if (user == null || user.getUserPersonalDetails() == null || user.getUserPersonalDetails().getDob() == null) {
            return false;
        }
        return isDateWithinAgeLimit(
                user.getUserPersonalDetails().getDob(),
                OTHER_STAFF_EMPLOYEE_MAX_CLAIM_AGE,
                OTHER_STAFF_EMPLOYEE_EXTRA_ELIGIBLE_DAYS
        );
    }

    private boolean isDateWithinAgeLimit(Date dateOfBirth, int maxYears, int extraEligibleDays) {
        if (dateOfBirth == null) {
            return false;
        }
        Calendar eligibleUntil = Calendar.getInstance();
        eligibleUntil.setTime(dateOfBirth);
        eligibleUntil.add(Calendar.YEAR, maxYears);
        eligibleUntil.add(Calendar.DAY_OF_MONTH, extraEligibleDays);
        eligibleUntil.set(Calendar.HOUR_OF_DAY, 23);
        eligibleUntil.set(Calendar.MINUTE, 59);
        eligibleUntil.set(Calendar.SECOND, 59);
        eligibleUntil.set(Calendar.MILLISECOND, 999);
        return !DateTimeUtil.getCurrentDateTime().after(eligibleUntil.getTime());
    }

    private boolean hasCompletedNormalStaffCricPermanentPeriod(ApplicationUser user) {
        Date permanentDate = resolvePermanentDateForCricEligibility(user);
        if (permanentDate == null) {
            return false;
        }
        Calendar eligibleDate = Calendar.getInstance();
        eligibleDate.setTime(permanentDate);
        eligibleDate.add(Calendar.YEAR, NORMAL_STAFF_CRIC_MIN_PERMANENT_YEARS);
        return !DateTimeUtil.getCurrentDateTime().before(eligibleDate.getTime());
    }

    private Date resolvePermanentDateForCricEligibility(ApplicationUser user) {
        if (user == null
                || user.getUserPersonalDetails() == null
                || user.getUserPersonalDetails().getUserCompanyDetails() == null) {
            return null;
        }
        UserCompanyDetails companyDetails = user.getUserPersonalDetails().getUserCompanyDetails();
        return companyDetails.getPreviousPermanentDate() != null
                ? companyDetails.getPreviousPermanentDate()
                : companyDetails.getPermanentDate();
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

            String staffCategoryCode = user.getUserPersonalDetails()
                    .getUserCompanyDetails()
                    .getStaffCategories()
                    .getCode();

            boolean isCRIC = insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode().equals(TreatmentType.CRIC.name());

            if ("NS".equals(staffCategoryCode) && !isNormalStaffEmployeeWithinMedicalAgeLimit(user)) {
                log.info("Normal staff employee age limit exceeded user={} ", user.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1047,
                        messageSource.getMessage(ResponseMessageUtil.CLAIM_NORMAL_STAFF_EMPLOYEE_AGE_LIMIT_EXCEED, null, locale)));
            }

            if (!"NS".equals(staffCategoryCode) && !isOtherStaffEmployeeWithinMedicalAgeLimit(user)) {
                log.info("Other staff employee age limit exceeded user={} ", user.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1047,
                        messageSource.getMessage(ResponseMessageUtil.CLAIM_OTHER_STAFF_EMPLOYEE_AGE_LIMIT_EXCEED, null, locale)));
            }

            if (isCRIC && "NS".equals(staffCategoryCode) && !hasCompletedNormalStaffCricPermanentPeriod(user)) {
                log.info("Normal staff CRIC approval blocked. Three years permanent employment not completed user={}",
                        user.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1054,
                        messageSource.getMessage(ResponseMessageUtil.NORMAL_STAFF_CRIC_PERMANENT_PERIOD_NOT_COMPLETED, null, locale)));
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

                if (isParentClaimBlockedForMedical(staffCategoryCode, user.getUserPersonalDetails().getMaritalStatus())
                        && claimsDependentsOpt.get().getDependentCategory().equals(DependentCategory.PARENTS)) {
                    log.info("Parent dependent claim is not allowed for staff category {}", staffCategoryCode);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1049,
                            messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                }

                if (!isParentWithinNormalStaffMedicalAgeLimit(user, claimsDependentsOpt.get())) {
                    log.info("Parent dependent claim is not allowed because age exceeds {} years and {} days",
                            NORMAL_STAFF_PARENT_MAX_CLAIM_AGE, NORMAL_STAFF_PARENT_EXTRA_ELIGIBLE_DAYS);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1049,
                            messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                }

                if (!isChildWithinMedicalAgeLimit(claimsDependentsOpt.get())) {
                    log.info("Child dependent claim is not allowed because age exceeds {} years and {} days",
                            CHILD_MAX_CLAIM_AGE, CHILD_EXTRA_ELIGIBLE_DAYS);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1049,
                            messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                }

                if (!isSpouseWithinMedicalAgeLimit(staffCategoryCode, claimsDependentsOpt.get())) {
                    int maxAge = resolveSpouseClaimMaxAge(staffCategoryCode);
                    log.info("Spouse dependent claim is not allowed because age reaches or exceeds {}", maxAge);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1049,
                            messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                }

                boolean isDeathClaimExists = deathClaimRequestRepository.existsByClaimsDependentsAndEmployeeAndRequestStatusIn(
                        claimsDependentsOpt.get(), user, List.of(Workflow.APPROVED));
                if (isDeathClaimExists) {
                    log.info("Claim dependent death claim request already approved");
                    return ResponseEntity.ok().body(responseUtil.error(null, 1047,
                            messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_DEATH_REQUEST_ALREADY_PROCEED, null, locale)));
                }
            }

            InsuranceStaffCategoryPeriod carryForwardPeriod = resolvePreviousPeriodForCarry(
                    user,
                    insuranceStaffCategoryPeriod,
                    insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode());
            BigDecimal sumOfClaims = rejoinCarryForwardService.getApprovedAmountByTreatment(
                    user,
                    insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode(),
                    insuranceStaffCategoryPeriod.getId(),
                    carryForwardPeriod
            );

            sumOfClaims = sumOfClaims != null ? sumOfClaims : BigDecimal.ZERO;

            InsuranceDetailsLimit insuranceDetailsLimit = insuranceClaimsRequest.getInsuranceDetailsLimit();
            InsuranceQuarter insuranceQuarter = insuranceClaimsRequest.getInsuranceQuarter();
            Date permanentDate = rejoinCarryForwardService.resolveEffectivePermanentDateForLimit(user);
            Map<String, BigDecimal> categoryFundLimits = resolveCategoryFundLimits(insuranceDetailsLimit, permanentDate);
            Map<String, BigDecimal> categoryApprovedSums = resolveCategoryApprovedSums(
                    user,
                    insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode(),
                    insuranceStaffCategoryPeriod.getId(),
                    carryForwardPeriod,
                    categoryFundLimits.keySet()
            );
            sumOfClaims = resolveEffectiveTreatmentApprovedSum(sumOfClaims, categoryApprovedSums);
            BigDecimal treatmentFundLimit = resolveTreatmentFundLimit(insuranceDetailsLimit, categoryFundLimits);
            BigDecimal globalRemaining = subtractToZero(treatmentFundLimit, sumOfClaims);
            String claimCategoryCode = insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory().getCode();

            BigDecimal requestAmount = claimRequestDTO.getApprovedAmount();

            if (insuranceDetailsLimit.getIsQuarter()) {
                BigDecimal sumOfClaimsCategory = categoryApprovedSums.getOrDefault(claimCategoryCode, BigDecimal.ZERO);

                BigDecimal fundLimit = categoryFundLimits.getOrDefault(
                        claimCategoryCode,
                        insuranceQuarter != null && insuranceQuarter.getQuarterLimit() != null
                                ? insuranceQuarter.getQuarterLimit()
                                : treatmentFundLimit
                );
                BigDecimal categoryRemaining = subtractToZero(fundLimit, sumOfClaimsCategory);
                BigDecimal remainingBalance = globalRemaining.min(categoryRemaining);
                log.info("Quarter limit check -> treatment {}, category {}, period {}, sumApproved {}, fundLimit {}, remaining {}",
                        insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode(),
                        claimCategoryCode,
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
                            countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatusIn(
                                    TreatmentType.CRIC.name(),
                                    insuranceStaffCategoryPeriod.getId(),
                                    policyId,
                                    List.of(Workflow.APPROVED));
                    log.info("Dependent not eligible due to CRIC {} {}", sumOfClaims, requestEmp);

                    boolean exists = insuranceClaimsRequestRepository.
                            existsByEmployeeAndInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatus(
                                    user,
                                    TreatmentType.CRIC.name(),
                                    insuranceStaffCategoryPeriod.getId(),
                                    policyId,
                                    Workflow.APPROVED);

                    if (requestEmp >= NS_MAX_EMPLOYEE_REQUESTS
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
                            countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatusInAndEmployee(
                                    TreatmentType.CRIC.name(),
                                    insuranceStaffCategoryPeriod.getId(),
                                    policyId,
                                    List.of(Workflow.APPROVED),
                                    user);
                    log.info("Dependent not eligible due to CRIC {} {}", sumOfClaims, requestEmp);

                    if (requestEmp > 4 || sumOfClaims.compareTo(BigDecimal.valueOf(2000000)) > 0 || insuranceClaimsRequest.getRequestAmount().compareTo(BigDecimal.valueOf(500000)) > 0) {
                        log.info("Invalid claim limit {} {} ", sumOfClaims, requestEmp);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1034, messageSource.getMessage(ResponseMessageUtil.NORMAL_STAFF_CLAIM_LIMIT_OR_OUT_OF_EMPLOYEE_REQUEST_EXCEED, null, locale)));

                    }
                }

            } else {
                BigDecimal sumOfClaimsCategory = categoryApprovedSums.getOrDefault(claimCategoryCode, BigDecimal.ZERO);
                BigDecimal fundLimit = categoryFundLimits.getOrDefault(
                        claimCategoryCode,
                        insuranceQuarter != null && insuranceQuarter.getQuarterLimit() != null
                                ? insuranceQuarter.getQuarterLimit()
                                : treatmentFundLimit
                );
                BigDecimal categoryRemaining = subtractToZero(fundLimit, sumOfClaimsCategory);
                BigDecimal remainingBalance = globalRemaining.min(categoryRemaining);

                if (insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory().getCode().equals(TreatmentCategory.OTHER.name())) {
                    if (requestAmount.compareTo(remainingBalance) > 0) {
                        log.info("Request fund limit exceeded for OTHER treatment");
                        return ResponseEntity.ok().body(responseUtil.error(null, 1052,
                                messageSource.getMessage(ResponseMessageUtil.CLAIM_LIMIT_EXCEED_WITH_LIMIT, new Object[]{remainingBalance}, locale)));
                    }

                } else {
                    if (requestAmount.compareTo(remainingBalance) > 0) {
                        log.info("Request exceeds treatment category limit");
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
            return insuranceClaimsRequestRepository.findById(claimRequestDTO.getId())
                    .filter(claimsRequest -> canAccess(claimsRequest, claimRequestDTO.getUsername()))
                    .map(claimsRequest -> {
                ClaimsRequestResponseDTO claimsRequestResponseDTO = claimsApprovalEntityToDto.mapClaimsApproval(claimsRequest, true);

                Date currentDate = DateTimeUtil.getCurrentDateTime();
                UserCompanyDetails companyDetails = claimsRequest.getEmployee().getUserPersonalDetails().getUserCompanyDetails();
                Date permanentDate = rejoinCarryForwardService.resolveEffectivePermanentDateForLimit(claimsRequest.getEmployee());

                List<InsuranceStaffCategoryPeriod> policyPeriods =
                        resolvePolicyPeriods(currentDate, permanentDate, companyDetails);
                Map<String, Object> limit = new LinkedHashMap<>();
                List<SimpleBaseDTO> policyList = new ArrayList<>();

                Map<Long, InsurancePolicy> claimPolicyByPeriod =
                        resolveClaimPolicyByPeriod(claimsRequest.getEmployee());
                Treatment treatment = claimsRequest.getInsuranceClaimsDetails().getTreatment();

                for (InsuranceStaffCategoryPeriod period : policyPeriods) {
                    SimpleBaseDTO periodDto = new SimpleBaseDTO(String.valueOf(period.getId()),
                            period.getFromDate().toString() + " to " + period.getToDate().toString() + " " + period.getStaffCategories().getDescription());
                    policyList.add(periodDto);

                    Map<String, AvailableInsuranceLimitDTO> periodLimits = new HashMap<>();
                    InsurancePolicy insurancePolicy =
                            resolveInsurancePolicyForPeriod(companyDetails, period, claimPolicyByPeriod);
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

    private List<InsuranceStaffCategoryPeriod> resolvePolicyPeriods(Date currentDate,
                                                                    Date permanentDate,
                                                                    UserCompanyDetails companyDetails) {
        if (companyDetails == null || companyDetails.getStaffCategories() == null) {
            return Collections.emptyList();
        }

        String staffCode = companyDetails.getStaffCategories().getCode();
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

        List<InsuranceStaffCategoryPeriod> periods = new ArrayList<>(insuranceStaffCategoryPeriodRepository
                .findByStaffCategories_CodeAndStatusAndFromDateGreaterThanEqualAndFromDateLessThanEqualOrderByFromDateAsc(
                        staffCode, Status.ACTIVE, startDate, currentPeriod.getFromDate()));

        StaffCategories previousStaffCategory = companyDetails.getPreviousStaffCategories();
        Date transferDate = companyDetails.getTransferDate();
        if (previousStaffCategory != null && transferDate != null) {
            periods.removeIf(period -> period.getFromDate().before(transferDate));
            String previousStaffCode = previousStaffCategory.getCode();
            Date previousStartDate = permanentDate != null
                    ? insuranceStaffCategoryPeriodRepository
                    .findByDateWithinRangeExclusiveEnd(permanentDate, previousStaffCode)
                    .filter(period -> period.getStatus() == Status.ACTIVE)
                    .map(InsuranceStaffCategoryPeriod::getFromDate)
                    .orElseGet(() -> insuranceStaffCategoryPeriodRepository
                            .findFirstByStaffCategories_CodeAndStatusAndFromDateAfterOrderByFromDateAsc(
                                    previousStaffCode, Status.ACTIVE, permanentDate)
                            .map(InsuranceStaffCategoryPeriod::getFromDate)
                            .orElse(startDate))
                    : startDate;
            Date previousPeriodEnd = new Date(Math.min(
                    currentPeriod.getFromDate().getTime(),
                    transferDate.getTime() - 1
            ));
            if (!previousPeriodEnd.before(previousStartDate)) {
                periods.addAll(insuranceStaffCategoryPeriodRepository
                        .findByStaffCategories_CodeAndStatusAndFromDateGreaterThanEqualAndFromDateLessThanEqualOrderByFromDateAsc(
                                previousStaffCode, Status.ACTIVE, previousStartDate, previousPeriodEnd));
            }
        }

        if (periods.isEmpty()) {
            return List.of(currentPeriod);
        }

        return periods.stream()
                .collect(Collectors.toMap(
                        InsuranceStaffCategoryPeriod::getId,
                        period -> period,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .values()
                .stream()
                .sorted(Comparator.comparing(InsuranceStaffCategoryPeriod::getFromDate))
                .toList();
    }

    private Map<Long, InsurancePolicy> resolveClaimPolicyByPeriod(ApplicationUser employee) {
        if (employee == null) {
            return Map.of();
        }
        Map<Long, InsurancePolicy> policies = new HashMap<>();
        for (InsuranceClaimsRequest claim : insuranceClaimsRequestRepository.findAllByEmployee(employee)) {
            InsuranceDetailsLimit detailsLimit = claim.getInsuranceDetailsLimit();
            if (detailsLimit == null
                    || detailsLimit.getInsuranceStaffCategoryPeriod() == null
                    || detailsLimit.getInsurancePolicy() == null) {
                continue;
            }
            policies.putIfAbsent(
                    detailsLimit.getInsuranceStaffCategoryPeriod().getId(),
                    detailsLimit.getInsurancePolicy());
        }
        return policies;
    }

    private InsurancePolicy resolveInsurancePolicyForPeriod(UserCompanyDetails companyDetails,
                                                            InsuranceStaffCategoryPeriod period,
                                                            Map<Long, InsurancePolicy> claimPolicyByPeriod) {
        Date transferDate = companyDetails.getTransferDate();
        boolean previousPeriod = transferDate != null && period.getFromDate().before(transferDate);
        if (previousPeriod && companyDetails.getPreviousInsurancePolicy() != null) {
            return companyDetails.getPreviousInsurancePolicy();
        }
        InsurancePolicy claimPolicy = claimPolicyByPeriod.get(period.getId());
        if (previousPeriod && claimPolicy != null) {
            return claimPolicy;
        }
        return companyDetails.getInsurancePolicy();
    }

    @Transactional(readOnly = true)
    public void setLimitMap(Map<String, AvailableInsuranceLimitDTO> limitMap,
                            InsuranceDetailsLimit insuranceDetailsLimit,
                            ApplicationUser applicationUser,
                            InsuranceClaimsRequest insuranceClaimsRequest) throws ParseException {

        try {
            log.info("Insurance ref {}", insuranceDetailsLimit.getId());

            Date permentDateTime = rejoinCarryForwardService.resolveEffectivePermanentDateForLimit(applicationUser);

            InsuranceStaffCategoryPeriod currentPeriod = insuranceDetailsLimit.getInsuranceStaffCategoryPeriod();
            String treatmentCode = insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatment().getTreatmentCode();
            Long periodId = currentPeriod != null ? currentPeriod.getId() : null;

            InsuranceStaffCategoryPeriod prevPeriod = resolvePreviousPeriodForCarry(applicationUser, currentPeriod, treatmentCode);
            BigDecimal sum = periodId != null
                    ? rejoinCarryForwardService.getApprovedAmountByTreatment(
                    applicationUser,
                    treatmentCode,
                    periodId,
                    prevPeriod
            )
                    : BigDecimal.ZERO;
            log.info("CLAIM_APPROVAL_LIMIT periodId={}, sumCurrent={}", periodId, sum);

            AvailableInsuranceLimitDTO dto = limitMap.get("limit");
            if (dto == null) {
                dto = new AvailableInsuranceLimitDTO();
            }

            List<InsuranceQuarter> quarters = insuranceDetailsLimit.getInsuranceQuarters();
            boolean usesQuarterLimits = Boolean.TRUE.equals(insuranceDetailsLimit.getIsQuarter());
            if (!usesQuarterLimits || quarters == null || quarters.isEmpty()) {
                BigDecimal fundLimit = insuranceDetailsLimit.getGlobalLimit() != null
                        ? insuranceDetailsLimit.getGlobalLimit()
                        : BigDecimal.ZERO;
                BigDecimal availableLimit = fundLimit.subtract(sum);
                if (availableLimit.compareTo(BigDecimal.ZERO) < 0) {
                    availableLimit = BigDecimal.ZERO;
                }
                dto.setFundLimit(fundLimit);
                dto.setAvailableLimit(availableLimit);
                limitMap.put("limit", dto);
                return;
            }

            InsuranceQuarter referenceQuarter = selectQuarterByPermanentDate(quarters, permentDateTime);
            Date rangeFrom = referenceQuarter != null ? referenceQuarter.getFromDate() : null;
            Date rangeTo = referenceQuarter != null ? referenceQuarter.getToDate() : null;

            Map<String, InsuranceQuarter> categoryQuarterMap = resolveCategoryQuarterMap(quarters, rangeFrom, rangeTo);
            String claimCategoryCode = insuranceClaimsRequest.getInsuranceClaimsDetails() != null
                    && insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory() != null
                    ? insuranceClaimsRequest.getInsuranceClaimsDetails().getTreatmentCategory().getCode()
                    : null;

            Map<String, BigDecimal> categoryFundLimits = resolveCategoryFundLimits(insuranceDetailsLimit, permentDateTime);
            if (categoryFundLimits.isEmpty()) {
                for (Map.Entry<String, InsuranceQuarter> entry : categoryQuarterMap.entrySet()) {
                    categoryFundLimits.put(entry.getKey(), resolveQuarterFundLimit(insuranceDetailsLimit, entry.getValue()));
                }
            }
            Map<String, BigDecimal> categoryApprovedSums = resolveCategoryApprovedSums(
                    applicationUser,
                    treatmentCode,
                    periodId,
                    prevPeriod,
                    categoryFundLimits.keySet());
            sum = resolveEffectiveTreatmentApprovedSum(sum, categoryApprovedSums);

            BigDecimal fundLimit = claimCategoryCode != null
                    ? categoryFundLimits.getOrDefault(claimCategoryCode, BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            BigDecimal treatmentFundLimit = resolveTreatmentFundLimit(insuranceDetailsLimit, categoryFundLimits);
            BigDecimal availableLimit = calculateCategoryAvailableLimit(
                    claimCategoryCode,
                    treatmentFundLimit,
                    sum,
                    categoryFundLimits,
                    categoryApprovedSums
            );

            log.info("CLAIM_APPROVAL_LIMIT periodId={}, category={}, fundLimit={}, available={}, categorySums={}",
                    periodId, claimCategoryCode, fundLimit, availableLimit, categoryApprovedSums);

            dto.setFundLimit(fundLimit);
            dto.setAvailableLimit(availableLimit);
            limitMap.put("limit", dto);

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private BigDecimal getApprovedCategorySum(ApplicationUser applicationUser,
                                              String treatmentCode,
                                              String categoryCode,
                                              Long periodId,
                                              InsuranceStaffCategoryPeriod prevPeriod) {
        if (periodId == null) {
            return BigDecimal.ZERO;
        }
        return rejoinCarryForwardService.getApprovedAmountByTreatmentCategory(
                applicationUser,
                treatmentCode,
                categoryCode,
                periodId,
                prevPeriod
        );
    }

    private Map<String, BigDecimal> resolveCategoryApprovedSums(ApplicationUser applicationUser,
                                                                String treatmentCode,
                                                                Long periodId,
                                                                InsuranceStaffCategoryPeriod prevPeriod,
                                                                java.util.Set<String> categoryCodes) {
        Map<String, BigDecimal> categoryApprovedSums = new LinkedHashMap<>();
        for (String categoryCode : categoryCodes) {
            categoryApprovedSums.put(
                    categoryCode,
                    getApprovedCategorySum(applicationUser, treatmentCode, categoryCode, periodId, prevPeriod)
            );
        }
        return categoryApprovedSums;
    }

    private BigDecimal resolveEffectiveTreatmentApprovedSum(BigDecimal directTreatmentApprovedSum,
                                                            Map<String, BigDecimal> categoryApprovedSums) {
        BigDecimal safeDirectSum = directTreatmentApprovedSum != null ? directTreatmentApprovedSum : BigDecimal.ZERO;
        BigDecimal categoryTotal = categoryApprovedSums.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return safeDirectSum.max(categoryTotal);
    }

    private InsuranceStaffCategoryPeriod resolvePreviousPeriodForCarry(ApplicationUser applicationUser,
                                                                       InsuranceStaffCategoryPeriod currentPeriod,
                                                                       String treatmentCode) {
        Date previousPermanentDate = applicationUser.getUserPersonalDetails()
                .getUserCompanyDetails()
                .getPreviousPermanentDate();
        Date changeDate = previousPermanentDate != null
                ? applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate()
                : null;
        if (currentPeriod == null || currentPeriod.getStaffCategories() == null) {
            return null;
        }

        InsuranceStaffCategoryPeriod previousPeriod = resolvePreviousPeriodFromClaimHistory(
                applicationUser,
                treatmentCode,
                currentPeriod);
        if (previousPeriod == null && changeDate != null) {
            previousPeriod = insuranceStaffCategoryPeriodRepository
                    .findByDateWithinRangeAnyStaff(changeDate)
                    .stream()
                    .filter(p -> p.getStaffCategories() != null)
                    .filter(p -> !p.getStaffCategories().getCode()
                            .equals(currentPeriod.getStaffCategories().getCode()))
                    .findFirst()
                    .orElse(null);
        }

        return previousPeriod;
    }

    private InsuranceStaffCategoryPeriod resolvePreviousPeriodFromClaimHistory(ApplicationUser applicationUser,
                                                                              String treatmentCode,
                                                                              InsuranceStaffCategoryPeriod currentPeriod) {
        if (applicationUser == null
                || currentPeriod == null
                || currentPeriod.getStaffCategories() == null
                || treatmentCode == null) {
            return null;
        }

        String currentStaffCode = currentPeriod.getStaffCategories().getCode();
        return insuranceClaimsRequestRepository
                .findAllByEmployeeAndRequestStatusIn(applicationUser, List.of(Workflow.APPROVED))
                .stream()
                .filter(claim -> claim.getInsuranceClaimsDetails() != null)
                .filter(claim -> claim.getInsuranceClaimsDetails().getTreatment() != null)
                .filter(claim -> treatmentCode.equalsIgnoreCase(
                        claim.getInsuranceClaimsDetails().getTreatment().getTreatmentCode()))
                .map(this::resolveClaimPeriod)
                .filter(Objects::nonNull)
                .filter(claimPeriod -> claimPeriod.getId() != null && currentPeriod.getId() != null)
                .filter(claimPeriod -> !claimPeriod.getId().equals(currentPeriod.getId()))
                .filter(claimPeriod -> claimPeriod.getStaffCategories() != null)
                .filter(claimPeriod -> !currentStaffCode.equals(claimPeriod.getStaffCategories().getCode()))
                .filter(claimPeriod -> isOverlappingPeriod(claimPeriod, currentPeriod))
                .max(Comparator.comparing(InsuranceStaffCategoryPeriod::getFromDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private InsuranceStaffCategoryPeriod resolveClaimPeriod(InsuranceClaimsRequest claim) {
        if (claim.getInsuranceDetailsLimit() != null
                && claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod() != null) {
            return claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod();
        }
        if (claim.getInsuranceClaimsDetails() != null) {
            return claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod();
        }
        return null;
    }

    private boolean isOverlappingPeriod(InsuranceStaffCategoryPeriod candidate,
                                        InsuranceStaffCategoryPeriod currentPeriod) {
        if (candidate.getFromDate() == null || candidate.getToDate() == null
                || currentPeriod.getFromDate() == null || currentPeriod.getToDate() == null) {
            return true;
        }
        return !candidate.getToDate().before(currentPeriod.getFromDate())
                && !candidate.getFromDate().after(currentPeriod.getToDate());
    }

    private Map<String, InsuranceQuarter> resolveCategoryQuarterMap(List<InsuranceQuarter> quarters,
                                                                    Date rangeFrom,
                                                                    Date rangeTo) {
        Map<String, List<InsuranceQuarter>> byCategory = quarters.stream()
                .filter(q -> q.getTreatmentCategory() != null)
                .collect(java.util.stream.Collectors.groupingBy(q -> q.getTreatmentCategory().getCode()));

        Map<String, InsuranceQuarter> categoryQuarterMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<InsuranceQuarter>> entry : byCategory.entrySet()) {
            List<InsuranceQuarter> sorted = entry.getValue().stream()
                    .sorted(java.util.Comparator.comparing(InsuranceQuarter::getFromDate,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .toList();
            InsuranceQuarter categoryQuarter = matchQuarterRange(sorted, rangeFrom, rangeTo);
            if (categoryQuarter == null && !sorted.isEmpty()) {
                categoryQuarter = sorted.get(0);
            }
            if (categoryQuarter != null) {
                categoryQuarterMap.put(entry.getKey(), categoryQuarter);
            }
        }
        return categoryQuarterMap;
    }

    private BigDecimal resolveQuarterFundLimit(InsuranceDetailsLimit insuranceDetailsLimit, InsuranceQuarter quarter) {
        if (quarter == null) {
            return BigDecimal.ZERO;
        }
        if (quarter.getQuarterLimit() != null) {
            return quarter.getQuarterLimit();
        }
        return insuranceDetailsLimit.getGlobalLimit() != null
                ? insuranceDetailsLimit.getGlobalLimit()
                : BigDecimal.ZERO;
    }

    private Map<String, BigDecimal> resolveCategoryFundLimits(InsuranceDetailsLimit insuranceDetailsLimit,
                                                              Date permanentDate) {
        List<InsuranceDetailsLimit> matchingLimits = resolveMatchingInsuranceDetailsLimits(insuranceDetailsLimit);
        Set<String> categoryCodes = collectCategoryCodes(matchingLimits);
        Map<String, BigDecimal> categoryFundLimits = new LinkedHashMap<>();
        for (String categoryCode : categoryCodes) {
            InsuranceDetailsLimit categoryLimitSource = resolveInsuranceDetailsLimitForCategory(
                    matchingLimits,
                    categoryCode,
                    permanentDate);
            if (categoryLimitSource == null) {
                continue;
            }
            InsuranceQuarter categoryQuarter = resolveApplicableQuarter(
                    categoryLimitSource,
                    categoryCode,
                    permanentDate);
            BigDecimal fundLimit = resolveCategoryFundLimit(categoryLimitSource, categoryQuarter);
            if (fundLimit != null) {
                categoryFundLimits.put(categoryCode, fundLimit);
            }
        }
        return categoryFundLimits;
    }

    private List<InsuranceDetailsLimit> resolveMatchingInsuranceDetailsLimits(InsuranceDetailsLimit insuranceDetailsLimit) {
        if (insuranceDetailsLimit == null
                || insuranceDetailsLimit.getInsurancePolicy() == null
                || insuranceDetailsLimit.getInsuranceStaffCategoryPeriod() == null
                || insuranceDetailsLimit.getTreatment() == null) {
            return insuranceDetailsLimit != null ? List.of(insuranceDetailsLimit) : List.of();
        }

        List<InsuranceDetailsLimit> matchingLimits = insuranceDetailsLimitRepository
                .findAllByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(
                        insuranceDetailsLimit.getInsurancePolicy(),
                        Status.ACTIVE,
                        insuranceDetailsLimit.getInsuranceStaffCategoryPeriod(),
                        insuranceDetailsLimit.getTreatment().getTreatmentCode());
        if (matchingLimits == null || matchingLimits.isEmpty()) {
            return List.of(insuranceDetailsLimit);
        }
        return matchingLimits;
    }

    private Set<String> collectCategoryCodes(List<InsuranceDetailsLimit> insuranceDetailsLimits) {
        Set<String> categoryCodes = new LinkedHashSet<>();
        if (insuranceDetailsLimits == null) {
            return categoryCodes;
        }
        for (InsuranceDetailsLimit detailsLimit : insuranceDetailsLimits) {
            if (detailsLimit.getInsuranceQuarters() == null) {
                continue;
            }
            for (InsuranceQuarter quarter : detailsLimit.getInsuranceQuarters()) {
                if (quarter != null && quarter.getTreatmentCategory() != null) {
                    categoryCodes.add(quarter.getTreatmentCategory().getCode());
                }
            }
        }
        return categoryCodes;
    }

    private InsuranceDetailsLimit resolveInsuranceDetailsLimitForCategory(List<InsuranceDetailsLimit> insuranceDetailsLimits,
                                                                          String categoryCode,
                                                                          Date lookupDate) {
        if (insuranceDetailsLimits == null || insuranceDetailsLimits.isEmpty()) {
            return null;
        }

        for (InsuranceDetailsLimit detailsLimit : insuranceDetailsLimits) {
            if (resolveApplicableQuarter(detailsLimit, categoryCode, lookupDate) != null) {
                return detailsLimit;
            }
        }

        for (InsuranceDetailsLimit detailsLimit : insuranceDetailsLimits) {
            boolean categoryExists = detailsLimit.getInsuranceQuarters() != null
                    && detailsLimit.getInsuranceQuarters().stream()
                    .filter(Objects::nonNull)
                    .filter(quarter -> quarter.getTreatmentCategory() != null)
                    .anyMatch(quarter -> categoryCode.equalsIgnoreCase(quarter.getTreatmentCategory().getCode()));
            if (categoryExists) {
                return detailsLimit;
            }
        }
        return insuranceDetailsLimits.get(0);
    }

    private InsuranceQuarter resolveApplicableQuarter(InsuranceDetailsLimit insuranceDetailsLimit,
                                                      String categoryCode,
                                                      Date lookupDate) {
        InsuranceQuarter matchingQuarter = insuranceQuarterRepository
                .findByDateWithinRangeAndCodeWithLimit(insuranceDetailsLimit, categoryCode, lookupDate)
                .stream()
                .findFirst()
                .orElse(null);
        if (matchingQuarter != null) {
            return matchingQuarter;
        }

        InsuranceQuarter firstQuarter = insuranceQuarterRepository
                .findFirstByInsuranceDetailsLimitAndTreatmentCategory_CodeOrderByFromDateAsc(
                        insuranceDetailsLimit,
                        categoryCode)
                .orElse(null);
        if (firstQuarter == null || lookupDate == null || firstQuarter.getFromDate() == null) {
            return null;
        }
        return lookupDate.before(firstQuarter.getFromDate()) ? firstQuarter : null;
    }

    private BigDecimal resolveCategoryFundLimit(InsuranceDetailsLimit insuranceDetailsLimit,
                                                InsuranceQuarter insuranceQuarter) {
        if (!Boolean.TRUE.equals(insuranceDetailsLimit.getIsQuarter())) {
            return insuranceDetailsLimit.getGlobalLimit();
        }
        return insuranceQuarter != null ? insuranceQuarter.getQuarterLimit() : null;
    }

    private BigDecimal resolveTreatmentFundLimit(InsuranceDetailsLimit insuranceDetailsLimit,
                                                 Map<String, BigDecimal> categoryFundLimits) {
        if (!Boolean.TRUE.equals(insuranceDetailsLimit.getIsQuarter())
                && insuranceDetailsLimit.getGlobalLimit() != null) {
            return insuranceDetailsLimit.getGlobalLimit();
        }

        return categoryFundLimits.values().stream()
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateCategoryAvailableLimit(String claimCategoryCode,
                                                       BigDecimal treatmentFundLimit,
                                                       BigDecimal treatmentApprovedSum,
                                                       Map<String, BigDecimal> categoryFundLimits,
                                                       Map<String, BigDecimal> categoryApprovedSums) {
        if (claimCategoryCode == null || !categoryFundLimits.containsKey(claimCategoryCode)) {
            return BigDecimal.ZERO;
        }

        BigDecimal claimFundLimit = categoryFundLimits.get(claimCategoryCode);
        if (claimFundLimit == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal categoryRemaining = subtractToZero(
                claimFundLimit,
                categoryApprovedSums.getOrDefault(claimCategoryCode, BigDecimal.ZERO)
        );
        BigDecimal treatmentRemaining = subtractToZero(treatmentFundLimit, treatmentApprovedSum);
        return treatmentRemaining.min(categoryRemaining);
    }

    private BigDecimal subtractToZero(BigDecimal fundLimit, BigDecimal usedAmount) {
        BigDecimal safeFundLimit = fundLimit != null ? fundLimit : BigDecimal.ZERO;
        BigDecimal safeUsedAmount = usedAmount != null ? usedAmount : BigDecimal.ZERO;
        BigDecimal remainingAmount = safeFundLimit.subtract(safeUsedAmount);
        return remainingAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingAmount;
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

    private boolean canAccess(InsuranceClaimsRequest claim, String username) {
        return claim != null
                && claim.getEmployee() != null
                && claim.getEmployee().getUserPersonalDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes() != null
                && companyAccessService.canAccess(username,
                claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode());
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

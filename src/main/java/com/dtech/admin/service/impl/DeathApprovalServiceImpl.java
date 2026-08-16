package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ClaimRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.DeathRequestResponseDTO;
import com.dtech.admin.dto.response.DependentResponseDTO;
import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.enums.DeathBeneficiary;
import com.dtech.admin.enums.TreatmentCategory;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.DeathApprovalAuditMapper;
import com.dtech.admin.mapper.entityToDto.DeathApprovalEntityToDto;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.DeathApprovalService;
import com.dtech.admin.service.CompanyAccessService;
import com.dtech.admin.service.MessageService;
import com.dtech.admin.specifications.DeathApprovalSpecification;
import com.dtech.admin.specifications.CompanyScopeSpecification;
import com.dtech.admin.util.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class DeathApprovalServiceImpl implements DeathApprovalService {

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
    private final RemarkRepository remarkRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final CompanyAccessService companyAccessService;

    @Autowired
    private final WebUserRepository webUserRepository;

    @Autowired
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final DeathApprovalEntityToDto deathApprovalEntityToDto;

    @Autowired
    private final ApprovalWorkFlowRepository approvalWorkFlowRepository;

    @Autowired
    private final MessageService messageService;

    @Autowired
    private final CommonParameterRepository commonParameterRepository;

    @Autowired
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private final DeathBeneficiaryRepository deathBeneficiaryRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {

            log.info("Approval death request {} ", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.DDFA.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Workflow.values())
                    .filter(status -> !Status.ACTIVE.name().equals(status.name()))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> remarks = remarkRepository.findAllByRemarkCategoryAndStatus(RemarkCategory.DEATH, Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> relationCategory = Arrays.stream(RelationCategory.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> staffCategory = staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            WebUser webUser = webUserRepository.findByUsername(channelRequestDTO.getUsername()).orElse(null);

            List<SimpleBaseDTO> companyTypes = companyAccessService.activeCompanies(channelRequestDTO.getUsername()).stream().map(
                    val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            Optional.ofNullable(webUser)
                    .map(WebUser::getDeathApprovalLevel)
                    .ifPresent(approvalLevel -> {
                        switch (approvalLevel) {
                            case LEVEL01 -> privileges.setApprovalL1(true);
                            case LEVEL02 -> privileges.setApprovalL2(true);
                        }
                    });

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            responseMap.put("relationCategory", relationCategory);
            responseMap.put("remarks", remarks);
            responseMap.put("staffCategory", staffCategory);
            responseMap.put("company", companyTypes);

            auditLogService.log(WebPage.DDFA.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.DDFA.name()}, locale)));

        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> actionRequest(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Approval death request {} ", claimRequestDTO);

            Optional<DeathClaimRequest> optClaim = deathClaimRequestRepository.findById(claimRequestDTO.getId())
                    .filter(claim -> canAccess(claim, claimRequestDTO.getUsername()));
            if (optClaim.isEmpty()) {
                log.info("Death claim request does not exist: {}", claimRequestDTO.getId());
                return ResponseEntity.ok(responseUtil.error(null, 1044,
                        messageSource.getMessage(ResponseMessageUtil.DEATH_DETAILS_NOT_FOUND, new Object[]{claimRequestDTO.getId()}, locale)));
            }

            DeathClaimRequest claim = optClaim.get();

            Optional<WebUser> optUser = webUserRepository.findByUsername(claimRequestDTO.getUsername());
            if (optUser.isEmpty()) {
                log.info("User doesn't exist: {}", claimRequestDTO.getUsername());
                return ResponseEntity.ok(responseUtil.error(null, 1011,
                        messageSource.getMessage(ResponseMessageUtil.SYSTEM_USER_NOT_FOUND_OR_INACTIVE, new Object[]{claimRequestDTO.getUsername()}, locale)));
            }

            // Allow any approved amount for death claims; no upper-bound validation.

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

            if (claimRequestDTO.getStatus().equals(Workflow.APPROVED.name())) {
                ResponseEntity<ApiResponse<Object>> validation = claimsApprovalValidation(claim.getEmployee(), claim, locale);
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
                }

                case LEVEL02 -> {
                    claim.setRequestStatus(newStatus);
                    claim.setApprovedAmount(workFlow.getApprovedAmount());

                    MessageType messageType = MessageType.DEATH_REJECTED;
                    String otherMark = workFlow.getRejectedRemark() != null ? workFlow.getRejectedRemark() :"" ;

                    if(claim.getRequestStatus().equals(Workflow.APPROVED)) {
                        messageType = MessageType.DEATH_APPROVAL;
                        otherMark = String.valueOf(claim.getApprovedAmount());
                        if (claim.getClaimsDependents() != null) {
                            claim.getClaimsDependents().setLiveStatus(false);
                            claimDependentsRepository.saveAndFlush(claim.getClaimsDependents());
                        }
                    }

                    notifyMessage(claim.getEmployee().getPrimaryMobile(), claim.getRequestId(),messageType,otherMark);
                }
            }

            deathClaimRequestRepository.saveAndFlush(claim);

            return ResponseEntity.ok(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.DEATH_CLAIMS_APPROVED_SUCCESS, null, locale)));


        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }

    private ResponseEntity<ApiResponse<Object>> claimsApprovalValidation(ApplicationUser user, DeathClaimRequest deathClaimRequest, Locale locale) {

        try {

            log.info("Death claim approval validation started");

            if (user.getUserPersonalDetails().getIsTemp()
                    || user.getUserPersonalDetails().getUserCompanyDetails().getFacility().equals(Facility.DEATH)) {
                log.info("Claims request user not eligible {}", user.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1029,
                        messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
            }

            Optional<CommonParameter> paramOpt = commonParameterRepository.findByCode(CommonParam.DEATH_CLAIM_REQUEST_PERIOD.name());

            if (paramOpt.isEmpty()) {
                log.info("Common parameter not found for claim period");
                return ResponseEntity.ok().body(responseUtil.error(null, 1036,
                        messageSource.getMessage(ResponseMessageUtil.COMMON_PARAM_NOT_FOUND, null, locale)));
            }

            CommonParameter param = paramOpt.get();
            Date minuesDate = DateTimeUtil.getMinusDate(param.getValue() + 1, deathClaimRequest.getCreatedDate());

            if (deathClaimRequest.getDeathDate().before(minuesDate)) {
                log.info("Claim request is older than allowed {}", user.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1037,
                        messageSource.getMessage(ResponseMessageUtil.OLDER_DATE_INSURANCE_CLAIM_REQUEST, null, locale)));
            }

            Optional<ClaimsDependents> claimsDependentsOpt = Optional.empty();
            if (deathClaimRequest.getClaimsDependents() != null) {
                claimsDependentsOpt = claimDependentsRepository.findByIdAndApplicationUserAndStatusAndEligibleFacilityIn(
                        deathClaimRequest.getClaimsDependents().getId(), user, Workflow.APPROVED,
                        List.of(Facility.DEATH, Facility.BOTH));

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
            return null;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Async
    protected void notifyMessage(String mobile, String requestId,MessageType messageType, String otherMark) {
        try {
            messageService.sendMessage(messageType, requestId,otherMark, mobile);
            log.info("Sent OTP, waiting for response... sent death claims");
        } catch (RuntimeException e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ClaimRequestSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Death approval filter list {} ", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            Specification<DeathClaimRequest> specification = Objects.nonNull(paginationRequest.getSearch())
                    ? DeathApprovalSpecification.getSpecification(paginationRequest.getSearch(), false, true, false, true)
                    : DeathApprovalSpecification.getSpecification(false, true, false, true);
            specification = specification.and(CompanyScopeSpecification.companyCodeIn(
                    companyAccessService.activeCompanyCodes(paginationRequest.getUsername()),
                    "employee", "userPersonalDetails", "userCompanyDetails", "companyTypes", "code"));
            Page<DeathClaimRequest> deathClaimRequests = deathClaimRequestRepository.findAll(specification, pageable);
            log.info("Approval death details filter records {}", deathClaimRequests);
            long totalElements = deathClaimRequestRepository.count(specification);
            log.info("Approval death details filter records map start");

            List<DeathRequestResponseDTO> responseDTOList = deathClaimRequests.stream()
                    .map(claim -> deathApprovalEntityToDto.mapClaimsApproval(claim, false)).toList();

            log.info("Approval death details filter records map finish");
   //         List<String> newAuditList = deathApprovalAuditMapper.mapToDTOAudit(deathClaimRequests.stream().toList());
    //        auditLogService.log(WebPage.DDFA.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<DeathRequestResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.DEATH_DETAILS_FILTER_LIST_SUCCESSFULLY,
                            null, locale)));
        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> view(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Death approval view {} ", claimRequestDTO);
            return deathClaimRequestRepository.findById(claimRequestDTO.getId())
                    .filter(claimsRequest -> canAccess(claimsRequest, claimRequestDTO.getUsername()))
                    .map(claimsRequest -> {
                DeathRequestResponseDTO deathRequestResponseDTO = deathApprovalEntityToDto.mapClaimsApproval(claimsRequest, true);

                CommonParameter deathAge = commonParameterRepository.findByCode(CommonParam.DEATH_AGE.name()).orElse(null);
                CommonParameter childAgeMin = commonParameterRepository.findByCode(CommonParam.DDF_REQUEST_CHILDREN_MIN_AGE.name()).orElse(null);

                DependentResponseDTO claimsDependents = deathRequestResponseDTO.getClaimsDependents();
                if (claimsDependents != null) {
                    if (claimsDependents.getRelationCategory().equals(RelationCategory.CHILD.name()) || claimsDependents.getRelationCategory().equals(RelationCategory.SISTER.name())
                            || claimsDependents.getRelationCategory().equals(RelationCategory.BROTHER.name())) {
                        int childAge = DateTimeUtil.getAgeInDays(String.valueOf(claimsDependents.getDob()));
                        log.info("Child age {}", childAge);
                        if (childAge > (Objects.nonNull(childAgeMin) ? childAgeMin.getValue() : 0)) {
                            log.info("Age {} is greater than age child ", childAgeMin.getValue());
                            int age = DateTimeUtil.getAge(String.valueOf(claimsDependents.getDob()));
                            Range range = Range.LOWER;

                            if (age > (deathAge != null ? deathAge.getValue() : 1)) {
                                log.info("Upper range claim reference data {} ", age);
                                range = Range.UPPER;
                            }

                            com.dtech.admin.model.DeathBeneficiary deathBeneficiary = deathBeneficiaryRepository.
                                    findByCodeAndRangeAndStatus(DeathBeneficiary.valueOf(claimsDependents.getRelationCategory()),
                                            range, Status.ACTIVE).orElse(null);

                            deathRequestResponseDTO.setDeathLimit(deathBeneficiary != null ? deathBeneficiary.getClaimLimit() : null);

                        }

                    } else {
                        com.dtech.admin.model.DeathBeneficiary deathBeneficiary = deathBeneficiaryRepository.
                                findByCodeAndStatus(DeathBeneficiary.valueOf(claimsDependents.getRelationCategory()), Status.ACTIVE).orElse(null);

                        deathRequestResponseDTO.setDeathLimit(deathBeneficiary != null ? deathBeneficiary.getClaimLimit() : null);

                    }
                } else {
                    com.dtech.admin.model.DeathBeneficiary deathBeneficiary = deathBeneficiaryRepository.
                            findByCodeAndStatus(DeathBeneficiary.EMPLOYEE, Status.ACTIVE).orElse(null);
                    deathRequestResponseDTO.setDeathLimit(deathBeneficiary != null ? deathBeneficiary.getClaimLimit() : null);
                }


          //      List<String> newAuditList = deathApprovalAuditMapper.mapToDTOAudit(List.of(claimsRequest));
         //       auditLogService.log(WebPage.DDFA.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), claimRequestDTO.getIp(), claimRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, claimRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) deathRequestResponseDTO, messageSource.getMessage(ResponseMessageUtil.DEATH_DETAILS_RETRIEVE_SUCCESSFULLY, null, locale)));

            }).orElseGet(() -> {
                log.info("Claim death details not found {}", claimRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1043, messageSource.getMessage(ResponseMessageUtil.DEATH_DETAILS_NOT_FOUND, new Object[]{claimRequestDTO.getId()}, locale)));
            });

        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }

    private boolean canAccess(DeathClaimRequest claim, String username) {
        return claim != null
                && claim.getEmployee() != null
                && claim.getEmployee().getUserPersonalDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails() != null
                && claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes() != null
                && companyAccessService.canAccess(username,
                claim.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode());
    }
}

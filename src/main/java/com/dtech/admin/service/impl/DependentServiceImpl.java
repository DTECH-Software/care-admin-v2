package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.DependentRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.DependentDetailsResponseDTO;
import com.dtech.admin.dto.search.ClaimDependentSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.mapper.audit.DependentDetailsAuditMapper;
import com.dtech.admin.mapper.entityToDto.DependentDetailsMapperEntityToDto;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.DependentService;
import com.dtech.admin.service.EmailNotificationService;
import com.dtech.admin.service.MessageService;
import com.dtech.admin.specifications.DependentSpecification;
import com.dtech.admin.util.*;
import com.google.gson.Gson;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Stream;

@Service
@Log4j2
@RequiredArgsConstructor
public class DependentServiceImpl implements DependentService {

    private static final Set<String> DEPENDENT_APPROVAL_ADMIN_ROLE_CODES = Set.of(
            "DevTest", "SUPERADMIN", "APPROVER", "ADMIN", "CLAIMS_APPROVER", "W_CSA"
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
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private final DependentDetailsMapperEntityToDto dependentDetailsMapperEntityToDto;

    @Autowired
    private final DependentDetailsAuditMapper dependentDetailsAuditMapperEntityToDto;

    @Autowired
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final WebUserRepository webUserRepository;

    @Autowired
    private final EmailNotificationService emailNotificationService;

    @Autowired
    private final MessageService messageService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Dependent details ref data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.DPNM.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Workflow.values())
                    .filter(status -> !Workflow.ACTIVE.name().equals(status.name()))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> facility = Arrays.stream(Facility.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> dependentCategory = Arrays.stream(DependentCategory.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> relationCategory = Arrays.stream(RelationCategory.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> live = Stream.of(
                    new SimpleBaseDTO("true", "Live"),
                    new SimpleBaseDTO("false", "None-live")
            ).toList();

            List<SimpleBaseDTO> companyTypes = getEligibleCompanies(channelRequestDTO.getUsername());

            List<SimpleBaseDTO> staffCategories = staffCategoriesRepository.findAllByStatus(Status.ACTIVE).stream().map(
                    val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            responseMap.put("facility", facility);
            responseMap.put("dependentCategory", dependentCategory);
            responseMap.put("relationCategory", relationCategory);
            responseMap.put("liveStatus", live);
            responseMap.put("company", companyTypes);
            responseMap.put("staffCategories", staffCategories);

            auditLogService.log(WebPage.DPNM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.DPNM.name()}, locale)));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private List<SimpleBaseDTO> getEligibleCompanies(String username) {
        List<SimpleBaseDTO> defaultCompanies = companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription()))
                .toList();

        return webUserRepository.findByUsername(username)
                .map(user -> user.getCompanies().stream()
                        .filter(company -> Status.ACTIVE.equals(company.getStatus()))
                        .map(company -> new SimpleBaseDTO(company.getCode(), company.getDescription()))
                        .sorted(Comparator.comparing(SimpleBaseDTO::getCode))
                        .toList())
                .orElse(defaultCompanies);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ClaimDependentSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Dependent details filter data {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            Set<String> eligibleCompanyCodes = getEligibleCompanyCodes(paginationRequest.getUsername());

            Page<ClaimsDependents> claimsDependents = Objects.nonNull(paginationRequest.getSearch()) ?
                    claimDependentsRepository.findAll(DependentSpecification.getSpecification(paginationRequest.getSearch(), eligibleCompanyCodes), pageable) :
                    claimDependentsRepository.findAll(DependentSpecification.getSpecification(new ClaimDependentSearchDTO(), eligibleCompanyCodes), pageable);
            log.info("Dependent details filter records {}", claimsDependents);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    claimDependentsRepository.count(DependentSpecification.getSpecification(paginationRequest.getSearch(), eligibleCompanyCodes)) :
                    claimDependentsRepository.count(DependentSpecification.getSpecification(new ClaimDependentSearchDTO(), eligibleCompanyCodes));
            log.info("Dependent details filter records map start");
            List<DependentDetailsResponseDTO> responseDTOList = claimsDependents.stream()
                    .map(dependentDetailsMapperEntityToDto::mapDependentDetails).toList();
            log.info("Dependent details filter records map finish");
            List<String> newAuditList = dependentDetailsAuditMapperEntityToDto.mapToDTOAudit(claimsDependents.stream().toList());
            auditLogService.log(WebPage.DPNM.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<DependentDetailsResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.DEPENDENT_DETAILS_FILTER_LIST_SUCCESSFULLY,
                            null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private Set<String> getEligibleCompanyCodes(String username) {
        return webUserRepository.findByUsername(username)
                .map(user -> user.getCompanies().stream()
                        .filter(company -> Status.ACTIVE.equals(company.getStatus()))
                        .map(company -> company.getCode())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(() -> companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                        .map(company -> company.getCode())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(DependentRequestDTO dependentRequestDTO, Locale locale) {
        try {
            log.info("Dependent details add data {}", dependentRequestDTO);
            return claimDependentsRepository
                    .findById(dependentRequestDTO.getId()).map(de -> {
                        Workflow previousStatus = de.getStatus();

                        String newModel = new StringBuilder()
                                .append(dependentRequestDTO.getStatus()).toString();

                        String oldModel = new StringBuilder()
                                .append(de.getStatus().name()).toString();

                        if (oldModel.equals(newModel)) {
                            log.info("Dependent details update data not changed to {}", newModel);
                            return ResponseEntity.ok().body(responseUtil.error(null, 1044, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_CHANGING, null, locale)));
                        }

                        if(Workflow.valueOf(dependentRequestDTO.getStatus()).equals(Workflow.APPROVED)){
                            ResponseEntity<ApiResponse<Object>> validateApprovedDependent =
                                    validateApprovedDependent(de.getApplicationUser(), de, locale);

                            if(validateApprovedDependent != null){
                                log.info("Validation failed");
                                return validateApprovedDependent;
                            }

                        }

                        log.info("Dependent details update old audit start");
                        List<String> oldAuditList = dependentDetailsAuditMapperEntityToDto.mapToDTOAudit(List.of(de));
                        log.info("Dependent details update old audit end");

                        boolean isUpdate  = false;
                        if(Workflow.valueOf(dependentRequestDTO.getStatus()).equals(Workflow.APPROVED)){
                            de.setStatus(Workflow.APPROVED);
                            isUpdate = true;
                        }else if(Workflow.valueOf(dependentRequestDTO.getStatus()).equals(Workflow.REJECTED)){
                            de.setStatus(Workflow.REJECTED);
                            de.setRemark(dependentRequestDTO.getRemark());
                            isUpdate = true;
                        }

                        if(isUpdate){
                            de.setApprovedUser(dependentRequestDTO.getUsername());
                            de.setApprovedDate(DateTimeUtil.getCurrentDateTime());
                            log.info("Dependent details success");
                            claimDependentsRepository.saveAndFlush(de);
                            if (!Workflow.APPROVED.equals(previousStatus) && Workflow.APPROVED.equals(de.getStatus())) {
                                notifyAdminTeamOnDependentApproval(de, dependentRequestDTO.getUsername());
                                notifyEmployeeOnDependentApproval(de);
                            } else if (Workflow.REJECTED.equals(de.getStatus())) {
                                notifyEmployeeOnDependentRejection(de);
                            }
                        }

                        List<String> newAuditList = dependentDetailsAuditMapperEntityToDto.mapToDTOAudit(List.of(de));
                        auditLogService.log(WebPage.DPNM.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(), dependentRequestDTO.getIp(), dependentRequestDTO.getUserAgent(), gson.toJson(newAuditList), gson.toJson(oldAuditList), dependentRequestDTO.getUsername());
                        DependentDetailsResponseDTO responseDTO = dependentDetailsMapperEntityToDto.mapDependentDetails(de);
                        return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_DETAILS_UPDATE_SUCCESSFULLY, null, locale)));

                    }).orElseGet(() -> {
                        log.info("Dependent not found {}", dependentRequestDTO.getId());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1048, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_FOUND, new Object[]{dependentRequestDTO.getId()}, locale)));
                    });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private void notifyAdminTeamOnDependentApproval(ClaimsDependents dependent, String hrUsername) {
        String employeeCompanyCode = dependent.getApplicationUser() != null
                && dependent.getApplicationUser().getUserPersonalDetails() != null
                && dependent.getApplicationUser().getUserPersonalDetails().getUserCompanyDetails() != null
                && dependent.getApplicationUser().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes() != null
                ? dependent.getApplicationUser().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode()
                : null;

        List<WebUser> recipients = webUserRepository.findAllByStatus(Status.ACTIVE).stream()
                .filter(user -> user.getUserRole() != null && org.springframework.util.StringUtils.hasText(user.getUserRole().getCode()))
                .filter(user -> DEPENDENT_APPROVAL_ADMIN_ROLE_CODES.stream()
                        .anyMatch(roleCode -> roleCode.equalsIgnoreCase(user.getUserRole().getCode())))
                .filter(user -> !org.springframework.util.StringUtils.hasText(employeeCompanyCode)
                        || user.getCompanies() == null
                        || user.getCompanies().isEmpty()
                        || user.getCompanies().stream()
                        .anyMatch(company -> Status.ACTIVE.equals(company.getStatus())
                                && employeeCompanyCode.equalsIgnoreCase(company.getCode())))
                .toList();

        emailNotificationService.notifyDependentApprovedByHr(recipients, dependent, hrUsername);
    }

    private void notifyEmployeeOnDependentRejection(ClaimsDependents dependent) {
        try {
            String mobile = resolveEmployeeMobile(dependent);

            if (!StringUtils.hasText(mobile)) {
                log.warn("Skipping dependent rejection SMS. Employee mobile not found for dependent {}", dependent != null ? dependent.getId() : null);
                return;
            }

            String relation = Optional.ofNullable(dependent.getRelationCategory())
                    .map(RelationCategory::getDescription)
                    .orElse("Dependent");

            messageService.sendMessageAsync(MessageType.DEPENDENT_REJECTED, relation, "", mobile);
        } catch (Exception ex) {
            log.error("Failed to send dependent rejection SMS for dependent {}", dependent != null ? dependent.getId() : null, ex);
        }
    }

    private void notifyEmployeeOnDependentApproval(ClaimsDependents dependent) {
        try {
            String mobile = resolveEmployeeMobile(dependent);

            if (!StringUtils.hasText(mobile)) {
                log.warn("Skipping dependent approval SMS. Employee mobile not found for dependent {}", dependent != null ? dependent.getId() : null);
                return;
            }

            String relation = Optional.ofNullable(dependent.getRelationCategory())
                    .map(RelationCategory::getDescription)
                    .orElse("Dependent");

            messageService.sendMessageAsync(MessageType.DEPENDENT_APPROVED, relation, "", mobile);
        } catch (Exception ex) {
            log.error("Failed to send dependent approval SMS for dependent {}", dependent != null ? dependent.getId() : null, ex);
        }
    }

    private String resolveEmployeeMobile(ClaimsDependents dependent) {
        return Optional.ofNullable(dependent)
                .map(ClaimsDependents::getApplicationUser)
                .map(applicationUser -> {
                    if (StringUtils.hasText(applicationUser.getPrimaryMobile())) {
                        return applicationUser.getPrimaryMobile();
                    }
                    return applicationUser.getUserPersonalDetails() != null
                            ? applicationUser.getUserPersonalDetails().getMobileNo()
                            : null;
                })
                .orElse(null);
    }

    private ResponseEntity<ApiResponse<Object>> validateApprovedDependent(ApplicationUser applicationUser, ClaimsDependents claimDependentRequestDTO, Locale locale) {
        try {
            log.info("Validate user");

            if (!applicationUser.getUserPersonalDetails().getMaritalStatus().equals(MaritalStatus.MARRIED)) {
                if (claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.WIFE.name())
                        || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.HUSBAND.name())
                        || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())
                        || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())
                        || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.CHILD.name())
                ) {
                    log.info("User not eligible add wife or husband {} ", claimDependentRequestDTO.getRelationCategory());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1042, messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_WIFE_OR_HUSBAND_DEPENDENTS, null, locale)));
                }
            } else if (applicationUser.getUserPersonalDetails().getGender().equals(Gender.MALE) &&
                    claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.HUSBAND.name())) {
                log.info("Can't relation husband {} ", claimDependentRequestDTO.getRelationCategory());
                return ResponseEntity.ok().body(responseUtil.error(null, 1041, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_HUSBAND_CANT_ADDED, null, locale)));

            } else if (applicationUser.getUserPersonalDetails().getGender().equals(Gender.FEMALE) &&
                    claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.WIFE.name())) {
                log.info("Can't relation wife {} ", claimDependentRequestDTO.getRelationCategory());
                return ResponseEntity.ok().body(responseUtil.error(null, 1041, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_WIFE_CANT_ADDED, null, locale)));

            } else if ((claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.HUSBAND.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.FATHER.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.BROTHER.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())) && claimDependentRequestDTO.getGender().name().equalsIgnoreCase(Gender.FEMALE.name())
            ) {
                log.info("Gender is not male correct {}", claimDependentRequestDTO.getFirstName());
                return ResponseEntity.ok().body(responseUtil.error(null, 1042, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_GENDER_INCORRECT, null, locale)));

            } else if ((claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.WIFE.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.MOTHER.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.SISTER.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())) && claimDependentRequestDTO.getGender().name().equalsIgnoreCase(Gender.MALE.name())
            ) {
                log.info("Gender is not female correct {}", claimDependentRequestDTO.getFirstName());
                return ResponseEntity.ok().body(responseUtil.error(null, 1042, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_GENDER_INCORRECT,null, locale)));

            }

            if (claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.MOTHER.name())) {
                boolean claimsDependents = claimDependentsRepository
                        .existsAllByApplicationUserAndRelationCategoryAndStatusIn(applicationUser,
                                RelationCategory.MOTHER, List.of(Workflow.APPROVED));

                if (claimsDependents) {
                    log.info("User profile add dependent request already active mother {} ", claimsDependents);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_MOTHER_FOUND, null, locale)));
                }

            } else if (claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.FATHER.name())) {
                boolean claimsDependents = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusIn(applicationUser,
                        RelationCategory.FATHER, List.of(Workflow.APPROVED));
                if (claimsDependents) {
                    log.info("User profile add dependent request already active father {} ", claimsDependents);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_FATHER_FOUND,null, locale)));
                }
            } else if (claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.WIFE.name())) {

                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                        RelationCategory.WIFE, List.of(Workflow.APPROVED), claimDependentRequestDTO.getMarried().getId());
                if (existed) {
                    log.info("User profile add dependent request already married round wife {} ", existed);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_WIFE_MARRIED_ROUND_ALREADY_FOUND,null, locale)));
                }
            } else if (claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.HUSBAND.name())) {

                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                        RelationCategory.HUSBAND, List.of(Workflow.APPROVED),claimDependentRequestDTO.getMarried().getId());
                if (existed) {
                    log.info("User profile add dependent request already married round husband {} ", existed);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_HUSBAND_MARRIED_ROUND_ALREADY_FOUND,null, locale)));
                }
            } else if (claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.FATHER_IN_LAW.name())) {

                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                        RelationCategory.FATHER_IN_LAW, List.of(Workflow.APPROVED), claimDependentRequestDTO.getMarried().getId());
                if (existed) {
                    log.info("User profile add dependent request already married round father in law {} ", existed);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_FATHER_IN_LAW_MARRIED_ROUND_ALREADY_FOUND, null, locale)));
                }
            } else if (claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.MOTHER_IN_LAW.name())) {

                boolean existed = claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(applicationUser,
                        RelationCategory.MOTHER_IN_LAW, List.of(Workflow.APPROVED), claimDependentRequestDTO.getMarried().getId());
                if (existed) {
                    log.info("User profile add dependent request already married round mother in law {} ", existed);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_MOTHER_IN_LAW_MARRIED_ROUND_ALREADY_FOUND, null, locale)));
                }
            }

            if (claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.WIFE.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.HUSBAND.name())) {

                if (claimDependentRequestDTO.getDocuments().size() != 2) {
                    log.info("User profile add dependent request out of wife and husband document {} ", claimDependentRequestDTO);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1023, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_WIFE_DOCUMENT_IS_EMPTY_OR_OUT_OF_RANGE, null, locale)));

                } else {

                    boolean birth = claimDependentRequestDTO.getDocuments().stream().anyMatch(val -> val.getType().name().equals(DependentImageTypes.BIRTH.name()));

                    boolean married = claimDependentRequestDTO.getDocuments().stream().anyMatch(val -> val.getType().name().equals(DependentImageTypes.MARRIED.name()));

                    if (!birth && !married) {
                        log.info("Birth and married certificate missing");
                        return ResponseEntity.ok().body(responseUtil.error(null, 1040, messageSource.getMessage(ResponseMessageUtil.BIRTH_MARRIED_CERTIFICATE_MISSING, null, locale)));
                    } else if (!birth) {
                        log.info("Birth certificate missing");
                        return ResponseEntity.ok().body(responseUtil.error(null, 1040, messageSource.getMessage(ResponseMessageUtil.BIRTH_CERTIFICATE_MISSING, null, locale)));
                    } else if (!married) {
                        log.info("Married certificate missing");
                        return ResponseEntity.ok().body(responseUtil.error(null, 1040, messageSource.getMessage(ResponseMessageUtil.MARRIED_CERTIFICATE_MISSING, null, locale)));
                    }
                }
            } else if (claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.MOTHER.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.FATHER.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.CHILD.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.BROTHER.name())
                    || claimDependentRequestDTO.getRelationCategory().name().equalsIgnoreCase(RelationCategory.SISTER.name())) {

                if (claimDependentRequestDTO.getDocuments().size() != 1) {
                    log.info("User profile add dependent request out of parent or child document {} ", claimDependentRequestDTO);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1023, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_OTHER_RELATION_CATEGORY_DOCUMENT_IS_EMPTY_OR_OUT_OF_RANGE, null, locale)));
                } else {
                    boolean birth = claimDependentRequestDTO.getDocuments().stream().anyMatch(val -> val.getType().name().equals(DependentImageTypes.BIRTH.name()));
                    if (!birth) {
                        log.info("Birth certificate missing");
                        return ResponseEntity.ok().body(responseUtil.error(null, 1040, messageSource.getMessage(ResponseMessageUtil.BIRTH_CERTIFICATE_MISSING, null, locale)));
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
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(DependentRequestDTO dependentRequestDTO, Locale locale) {
        try {
            log.info("Dependent details view {}", dependentRequestDTO);
            return claimDependentsRepository
                    .findById(dependentRequestDTO.getId()).map(de -> {

                        DependentDetailsResponseDTO dependentDetailsResponseDTO = dependentDetailsMapperEntityToDto.mapDependentDetails(de);
                        List<String> newAuditList = dependentDetailsAuditMapperEntityToDto.mapToDTOAudit(List.of(de));
                        auditLogService.log(WebPage.DPNM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), dependentRequestDTO.getIp(), dependentRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, dependentRequestDTO.getUsername());
                        return ResponseEntity.ok().body(responseUtil.success((Object) dependentDetailsResponseDTO, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_DETAILS_RETRIEVE_SUCCESSFULLY, null, locale)));

                    }).orElseGet(() -> {
                        log.info("Dependent not found {}", dependentRequestDTO.getId());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1048, messageSource.getMessage(ResponseMessageUtil.DEPENDENT_NOT_FOUND, new Object[]{dependentRequestDTO.getId()}, locale)));
                    });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}

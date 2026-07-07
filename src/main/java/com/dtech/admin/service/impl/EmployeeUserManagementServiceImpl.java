package com.dtech.admin.service.impl;

import com.dtech.admin.dto.*;
import com.dtech.admin.dto.request.*;
import com.dtech.admin.dto.response.*;
import com.dtech.admin.dto.search.EmployeeSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.enums.DeathBeneficiary;
import com.dtech.admin.enums.MaritalStatus;
import com.dtech.admin.enums.TreatmentCategory;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.entityToDto.DependentDetailsMapperEntityToDto;
import com.dtech.admin.mapper.entityToDto.EmployeeUserMapperEntityToDto;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.EmailNotificationService;
import com.dtech.admin.service.EmployeeUserManagementService;
import com.dtech.admin.specifications.EmployeeUserSpecification;
import com.dtech.admin.util.*;
import com.google.gson.Gson;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
@Log4j2
@RequiredArgsConstructor
public class EmployeeUserManagementServiceImpl implements EmployeeUserManagementService {

    private static final Set<String> STAFF_CATEGORY_TRANSFER_ADMIN_ROLE_CODES = Set.of(
            "SUPERADMIN1", "SUPERADMIN", "ADMIN", "APPROVER", "DevTest", "SubAdmin"
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
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final StaffTypesRepository staffTypesRepository;

    @Autowired
    private final InsurancePolicyRepository insurancePolicyRepository;

    @Autowired
    private final InsurancePolicyStaffCategoryGroupRepository insurancePolicyStaffCategoryGroupRepository;

    @Autowired
    private final UserPersonalDetailsRepository userPersonalDetailsRepository;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final WebUserRepository webUserRepository;

    @Autowired
    private final EmployeeUserMapperEntityToDto employeeUserMapperEntityToDto;

    @Autowired
    private final DependentDetailsMapperEntityToDto dependentDetailsMapperEntityToDto;

    @Autowired
    private final InsuranceDetailsLimitRepository insuranceDetailsLimitRepository;

    @Autowired
    private final InsuranceStaffCategoryPeriodRepository insuranceStaffCategoryPeriodRepository;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final InsuranceQuarterRepository insuranceQuarterRepository;

    @Autowired
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final CommonParameterRepository commonParameterRepository;

    @Autowired
    private final DeathBeneficiaryRepository deathBeneficiaryRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private final EmailNotificationService emailNotificationService;

    @Autowired
    private final RejoinCarryForwardService rejoinCarryForwardService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Employee management ref data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.EPMP.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Status.values())
                    .filter(status -> !Status.DELETE.name().equals(status.name()))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> facility = Arrays.stream(Facility.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> companyTypes = getEligibleCompanies(channelRequestDTO.getUsername());

            List<SimpleBaseDTO> staffCategories = staffCategoriesRepository.findAllByStatus(Status.ACTIVE).stream()
                    .map(s -> new SimpleBaseDTO(s.getCode(), s.getDescription())).toList();

            List<SimpleBaseDTO> staffTypes = staffTypesRepository.findAllByStatus(Status.ACTIVE).stream()
                    .filter(st -> !st.getCode().equals("TEMP")).map(
                            s -> new SimpleBaseDTO(s.getCode(), s.getDescription())).toList();

            List<SimpleBaseDTO> insurancePolicy = insurancePolicyRepository.findAllByStatus(Status.ACTIVE).stream().map(
                    s -> new SimpleBaseDTO(s.getCode(), s.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            responseMap.put("companyTypes", companyTypes);
            responseMap.put("staffTypes", staffTypes);
            responseMap.put("staffCategories", staffCategories);
            responseMap.put("insurancePolicy", insurancePolicy);
            responseMap.put("facility", facility);

            auditLogService.log(WebPage.EPMP.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.EPMP.name()}, locale)));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Employee management filter data: {}", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            Set<String> eligibleCompanyCodes = getEligibleCompanyCodes(paginationRequest.getUsername());

            Page<ApplicationUser> applicationUsers = Objects.nonNull(paginationRequest.getSearch())
                    ? applicationUserRepository.findAll(EmployeeUserSpecification.getSpecification(paginationRequest.getSearch(), eligibleCompanyCodes), pageable)
                    : applicationUserRepository.findAll(EmployeeUserSpecification.getSpecification(eligibleCompanyCodes), pageable);

            log.info("Employee management filter records retrieved: {}", applicationUsers.getTotalElements());

            long totalElements = Objects.nonNull(paginationRequest.getSearch())
                    ? applicationUserRepository.count(EmployeeUserSpecification.getSpecification(paginationRequest.getSearch(), eligibleCompanyCodes))
                    : applicationUserRepository.count(EmployeeUserSpecification.getSpecification(eligibleCompanyCodes));

            log.info("Employee management filter records mapping started");

            List<ApplicationUserResponseDTO> responseDTOList = applicationUsers.stream()
                    .map(employeeUserMapperEntityToDto::mapEmployeeUserDetails)
                    .toList();

            log.info("Employee management filter records mapping finished");

            PagingResult<ApplicationUserResponseDTO> pagingResult = new PagingResult<>(responseDTOList, responseDTOList.size(), totalElements);

            String message = messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_USER_DETAILS_FILTER_LIST_SUCCESSFULLY, null, locale);

            return ResponseEntity.ok().body(responseUtil.success(pagingResult, message));
        } catch (Exception e) {
            log.error("Error filtering employee list", e);
            throw e;
        }
    }

    private List<SimpleBaseDTO> getEligibleCompanies(String username) {
        List<SimpleBaseDTO> defaultCompanies = companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                .map(company -> new SimpleBaseDTO(company.getCode(), company.getDescription()))
                .toList();

        return webUserRepository.findByUsername(username)
                .map(user -> user.getCompanies().stream()
                        .filter(company -> Status.ACTIVE.equals(company.getStatus()))
                        .map(company -> new SimpleBaseDTO(company.getCode(), company.getDescription()))
                        .sorted(Comparator.comparing(SimpleBaseDTO::getCode))
                        .toList())
                .orElse(defaultCompanies);
    }

    private Set<String> getEligibleCompanyCodes(String username) {
        return webUserRepository.findByUsername(username)
                .map(user -> user.getCompanies().stream()
                        .filter(company -> Status.ACTIVE.equals(company.getStatus()))
                        .map(company -> company.getCode())
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(() -> companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                        .map(company -> company.getCode())
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }


    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> getDependents(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale) {
        try {
            log.info("Get employee management dependent data {}", employeeManagementRequestDTO.getId());
            return applicationUserRepository.findById(employeeManagementRequestDTO.getId()).map(applicationUser -> {

                List<DependentDetailsResponseDTO> dependentDetailsResponseDTOS = applicationUser.getClaimsDependents().stream()
                        .map(dependentDetailsMapperEntityToDto::mapDependentDetails).toList();

                return ResponseEntity.ok().body(responseUtil.success((Object) dependentDetailsResponseDTOS, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_USER_DETAILS_DEPENDENT_RETRIEVE_SUCCESSFULLY, null, locale)));

            }).orElseGet(() -> {
                log.info("Employee management dependent data not found {}", employeeManagementRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1050, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_USER_DETAILS_NOT_FOUND, new Object[]{employeeManagementRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> getLimitDetails(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale) {
        try {
            log.info("Get fund limits data {}", employeeManagementRequestDTO.getId());

            return applicationUserRepository.findById(employeeManagementRequestDTO.getId()).map(user -> {

                InsuranceStaffCategoryPeriod period = insuranceStaffCategoryPeriodRepository.findByDateWithinRange(
                        DateTimeUtil.getCurrentDateTime(),
                        user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode()
                ).orElse(null);

                List<InsuranceDetailsLimit> insuranceDetailsLimits = insuranceDetailsLimitRepository
                        .findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriod(
                                user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                                Status.ACTIVE,
                                period
                        );

                List<SimpleBaseDTO> userWiseTreatment = new ArrayList<>();
                Map<String, List<SimpleBaseDTO>> userWiseTreatmentCategory = new HashMap<>();

                Map<String, Map<String, AvailableInsuranceLimitDTO>> internalLimitMap = new HashMap<>();

                insuranceDetailsLimits.forEach(in -> {
                    log.info("Processing treatment: {}", in.getTreatment().getTreatmentCode());

                    addIfNotPresent(userWiseTreatment, in);

                    String treatmentCode = in.getTreatment().getTreatmentCode();
                    userWiseTreatmentCategory.computeIfAbsent(treatmentCode, k -> new ArrayList<>());
                    addIfNotPresentTreatmentCategory(userWiseTreatmentCategory.get(treatmentCode), in);

                    try {
                        setLimitMap(internalLimitMap, in, user);
                    } catch (ParseException e) {
                        log.error("Error parsing limit: {}", e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                });

                Map<String, List<Map<String, AvailableInsuranceLimitDTO>>> transformedLimits = new HashMap<>();

                for (Map.Entry<String, Map<String, AvailableInsuranceLimitDTO>> treatmentEntry : internalLimitMap.entrySet()) {
                    String treatmentCode = treatmentEntry.getKey();
                    Map<String, AvailableInsuranceLimitDTO> categoryMap = treatmentEntry.getValue();

                    List<Map<String, AvailableInsuranceLimitDTO>> categoryList = new ArrayList<>();
                    for (Map.Entry<String, AvailableInsuranceLimitDTO> categoryEntry : categoryMap.entrySet()) {
                        Map<String, AvailableInsuranceLimitDTO> singleCategory = new HashMap<>();
                        singleCategory.put(categoryEntry.getKey(), categoryEntry.getValue());
                        categoryList.add(singleCategory);
                    }

                    transformedLimits.put(treatmentCode, categoryList);
                }

                List<ClaimsDependents> claimsDependents = claimDependentsRepository.
                        findByApplicationUserAndStatusAndEligibleFacilityInAndLiveStatus(user, Workflow.APPROVED, List.of(Facility.DEATH, Facility.BOTH), true)
                        .stream().filter(dep -> {
                            boolean exists = deathClaimRequestRepository.existsByClaimsDependentsAndEmployeeAndRequestStatusIn(dep, user, List.of(Workflow.APPROVED, Workflow.UNDER_REVIEW));
                            return !exists;
                        }).collect(Collectors.toList());

                int empAge = DateTimeUtil.getAge(String.valueOf(user.getUserPersonalDetails().getDob()));

                CommonParameter ddfAgeForDependent = commonParameterRepository.findByCode(CommonParam.EMPLOYEE_MAX_AGE_FOR_REQUEST_DDF.name()).orElse(null);

                if (empAge > (Objects.nonNull(ddfAgeForDependent) ? ddfAgeForDependent.getValue() : 0)) {
                    log.info("Age {} is greater than age ", empAge);
                    claimsDependents.clear();
                }

                CommonParameter deathAge = commonParameterRepository.findByCode(CommonParam.DEATH_AGE.name()).orElse(null);
                CommonParameter childAgeMin = commonParameterRepository.findByCode(CommonParam.DDF_REQUEST_CHILDREN_MIN_AGE.name()).orElse(null);

                ArrayList<DeathLimitDTO> deathLimitDTOS = new ArrayList<>();
                ArrayList<DependentBaseDTO> claimDependent = new ArrayList<>();

                claimsDependents.forEach((dep) -> {

                    if (dep.getRelationCategory().equals(RelationCategory.CHILD) || dep.getRelationCategory().equals(RelationCategory.SISTER)
                            || dep.getRelationCategory().equals(RelationCategory.BROTHER)) {
                        int childAge = DateTimeUtil.getAgeInDays(String.valueOf(dep.getDob()));
                        log.info("Child age {}", childAge);
                        if (childAge > (Objects.nonNull(childAgeMin) ? childAgeMin.getValue() : 0)) {
                            log.info("Age {} is greater than age child ", childAgeMin.getValue());
                            claimDependent.add(new DependentBaseDTO(String.valueOf(dep.getId()), dep.getFirstName() + " " + dep.getLastName(), dep.getRelationCategory().getDescription()));
                            log.info("Relation category {}", dep.getRelationCategory());

                            log.info("Relation claim reference data {} ", dep.getRelationCategory());
                            int age = DateTimeUtil.getAge(String.valueOf(dep.getDob()));
                            Range range = Range.LOWER;

                            if (age > (deathAge != null ? deathAge.getValue() : 1)) {
                                log.info("Upper range claim reference data {} ", age);
                                range = Range.UPPER;
                            }
                            com.dtech.admin.model.DeathBeneficiary deathBeneficiary = deathBeneficiaryRepository.
                                    findByCodeAndRangeAndStatus(DeathBeneficiary.valueOf(dep.getRelationCategory().name()),
                                            range, Status.ACTIVE).orElse(null);

                            deathLimitDTOS.add(DeathLimitDTO.builder()
                                    .dependentId(dep.getFirstName() + " " + dep.getLastName())
                                    .deathLimit(deathBeneficiary != null ? deathBeneficiary.getClaimLimit() : null)
                                    .ageRange(deathBeneficiary != null ? deathBeneficiary.getRange().name() : null)
                                    .build());

                        }

                    } else {

                        com.dtech.admin.model.DeathBeneficiary deathBeneficiary = deathBeneficiaryRepository.
                                findByCodeAndStatus(DeathBeneficiary.valueOf(dep.getRelationCategory().name()), Status.ACTIVE).orElse(null);

                        deathLimitDTOS.add(DeathLimitDTO.builder()
                                .dependentId(dep.getFirstName() + " " + dep.getLastName())
                                .deathLimit(deathBeneficiary != null ? deathBeneficiary.getClaimLimit() : null)
                                .ageRange(deathBeneficiary != null ? deathBeneficiary.getRange() != null ? deathBeneficiary.getRange().name() : null : null)
                                .build());

                    }

                });

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("insurance", transformedLimits);
                responseData.put("death", deathLimitDTOS);

                return ResponseEntity.ok().body(responseUtil.success(
                        (Object) responseData,
                        messageSource.getMessage("response.insurance.claim.reference.success", null, locale)
                ));

            }).orElseGet(() -> {
                log.info("Employee not found: {}", employeeManagementRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1050,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_USER_DETAILS_NOT_FOUND,
                                new Object[]{employeeManagementRequestDTO.getId()}, locale)));
            });

        } catch (Exception e) {
            log.error("Unexpected error in getLimitDetails", e);
            throw e;
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> view(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale) {
        try {
            log.info("Employee management view details request: {}", employeeManagementRequestDTO.toString());
            return applicationUserRepository.findById(employeeManagementRequestDTO.getId()).map(applicationUser -> {

                ApplicationUserResponseDTO applicationUserResponseDTO = buildEmployeeResponse(applicationUser);

                return ResponseEntity.ok().body(responseUtil.success((Object) applicationUserResponseDTO, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_USER_DETAILS_RETRIEVE_SUCCESSFULLY, null, locale)));

            }).orElseGet(() -> {
                log.info("Employee management details data not found {}", employeeManagementRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1050, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_USER_DETAILS_NOT_FOUND, new Object[]{employeeManagementRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void setLimitMap(Map<String, Map<String, AvailableInsuranceLimitDTO>> limitMap,
                            InsuranceDetailsLimit insuranceDetailsLimit,
                            ApplicationUser applicationUser) throws ParseException {


        try {
            log.info("Insurance ref {}", insuranceDetailsLimit.getId());

            for (InsuranceQuarter insuranceQuarter : insuranceDetailsLimit.getInsuranceQuarters()) {

                String category = insuranceQuarter.getTreatmentCategory().getCode();
                log.info("Insurance category {}", category);
                String treatmentCode = insuranceDetailsLimit.getTreatment().getTreatmentCode();
                Long insurancePeriod = insuranceDetailsLimit.getInsuranceStaffCategoryPeriod().getId();

                int currentYear = DateTimeUtil.getCurrentYear();
                log.info("Current year {}", currentYear);
                Date effectivePermanentDate = rejoinCarryForwardService.resolveEffectivePermanentDateForLimit(applicationUser);
                if (effectivePermanentDate == null) {
                    effectivePermanentDate = DateTimeUtil.getCurrentDateTime();
                }
                int year = DateTimeUtil.getYear(effectivePermanentDate);
                int month = DateTimeUtil.getMonth(effectivePermanentDate);
                log.info("Year {}", year);
                log.info("Month {}", month);

                BigDecimal funLimit = BigDecimal.valueOf(0.00);

                if (category.equals(TreatmentCategory.OTHER.name())) {

                    BigDecimal sum = insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                            applicationUser,
                            treatmentCode,
                            insurancePeriod,
                            List.of(Workflow.APPROVED)
                    );

                    if (insuranceDetailsLimit.getIsQuarter()) {
                        log.info("First request {} ", insuranceQuarter.getQuarterLimit());

                        Date permentDateTime = rejoinCarryForwardService.resolveEffectivePermanentDateForLimit(applicationUser);
                        if (permentDateTime == null) {
                            permentDateTime = DateTimeUtil.getCurrentDateTime();
                        }

                        InsuranceQuarter treatmentQuarter = insuranceQuarterRepository
                                .findByDateWithinRangeAndCodeWithLimit(insuranceDetailsLimit, TreatmentCategory.OTHER.name(), permentDateTime)
                                .stream()
                                .findFirst()
                                .orElse(null);

                        BigDecimal maxLimit = BigDecimal.valueOf(0.00);

                        if (treatmentQuarter != null) {
                            funLimit = treatmentQuarter.getQuarterLimit();

                            maxLimit = treatmentQuarter.getQuarterLimit();
                        } else {
                            funLimit = insuranceDetailsLimit.getGlobalLimit();

                            maxLimit = insuranceDetailsLimit.getGlobalLimit();
                        }

                        log.info("Sum amount insurance ref data {} {} ", sum, insuranceDetailsLimit.getGlobalLimit());
                        BigDecimal reValue = funLimit.subtract(sum != null ? sum : BigDecimal.valueOf(0.00));
                        BigDecimal remaining = reValue.compareTo(BigDecimal.ZERO) > 0 ? reValue : BigDecimal.valueOf(0.00);
                        log.info("Remaining amount insurance ref data {}", reValue);

                        limitMap
                                .computeIfAbsent(treatmentCode, k -> new HashMap<>())
                                .merge(category, new AvailableInsuranceLimitDTO(remaining, maxLimit),
                                        (existing, newDetails) -> new AvailableInsuranceLimitDTO(
                                                newDetails.getAvailableLimit(),
                                                newDetails.getFundLimit()
                                        ));
                    } else {
                        funLimit = insuranceDetailsLimit.getGlobalLimit();

                        BigDecimal maxLimit = insuranceDetailsLimit.getGlobalLimit();

                        log.info("Sum amount insurance ref data {} {}", sum, insuranceDetailsLimit.getGlobalLimit());
                        BigDecimal reValue = funLimit.subtract(sum != null ? sum : BigDecimal.valueOf(0.00));
                        BigDecimal remaining = reValue.compareTo(BigDecimal.ZERO) > 0 ? reValue : BigDecimal.valueOf(0.00);
                        log.info("Remaining amount insurance ref data {}", reValue);

                        limitMap
                                .computeIfAbsent(treatmentCode, k -> new HashMap<>())
                                .merge(category, new AvailableInsuranceLimitDTO(remaining, maxLimit),
                                        (existing, newDetails) -> new AvailableInsuranceLimitDTO(
                                                newDetails.getAvailableLimit(),
                                                newDetails.getFundLimit()
                                        ));
                    }

                } else {
                    log.info("Event period but dental or Spec");

                    BigDecimal gSum = insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                            applicationUser,
                            treatmentCode,
                            insurancePeriod,
                            List.of(Workflow.APPROVED)
                    );

                    BigDecimal maxLimit = insuranceQuarter.getQuarterLimit();

                    if (gSum == null) {
                        funLimit = insuranceQuarter.getQuarterLimit();
                    } else {
                        log.info("Remaining amount insurance ref data {} {} ", gSum, maxLimit);
                        BigDecimal gFLimit = insuranceDetailsLimit.getGlobalLimit();
                        if (gFLimit.compareTo(gSum) == 0) {
                            funLimit = BigDecimal.ZERO;
                            gSum = null;
                        } else {

                            BigDecimal sum = insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
                                    applicationUser,
                                    treatmentCode,
                                    insuranceQuarter.getTreatmentCategory().getCode(),
                                    insurancePeriod,
                                    List.of(Workflow.APPROVED)
                            );

                            if (sum != null) {
                                if (maxLimit.compareTo(sum) == 0) {
                                    funLimit = BigDecimal.valueOf(0.00);
                                    gSum = null;
                                } else if (maxLimit.compareTo(sum) > 0) {
                                    funLimit = maxLimit.subtract(sum);
                                    gSum = null;
                                } else {
                                    funLimit = gFLimit.subtract(gSum);
                                    gSum = null;
                                }

                            } else {

                                funLimit = gFLimit.subtract(gSum);
                                if (funLimit.compareTo(maxLimit) > 0) {
                                    funLimit = maxLimit;
                                }

                                gSum = null;
                            }
                        }
                    }

                    BigDecimal reValue = funLimit.subtract(gSum != null ? gSum : BigDecimal.valueOf(0.00));
                    BigDecimal remaining = reValue.compareTo(BigDecimal.ZERO) > 0 ? reValue : BigDecimal.valueOf(0.00);
                    log.info("Remaining amount insurance ref data {}", reValue);

                    limitMap
                            .computeIfAbsent(treatmentCode, k -> new HashMap<>())
                            .merge(category, new AvailableInsuranceLimitDTO(remaining, maxLimit),
                                    (existing, newDetails) -> new AvailableInsuranceLimitDTO(
                                            newDetails.getAvailableLimit(),
                                            newDetails.getFundLimit()
                                    ));
                }

            }
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale) {
        try {
            log.info("Employee user status update request {}", employeeManagementRequestDTO);

            return applicationUserRepository.findById(employeeManagementRequestDTO.getId()).map(applicationUser -> {
                boolean changed = false;

                if (hasText(employeeManagementRequestDTO.getLoginStatus())) {
                    applicationUser.setLoginStatus(Status.valueOf(employeeManagementRequestDTO.getLoginStatus()));
                    changed = true;
                }

                if (hasText(employeeManagementRequestDTO.getUserStatus())) {
                    if (applicationUser.getUserPersonalDetails() != null) {
                        applicationUser.getUserPersonalDetails().setUserStatus(Status.valueOf(employeeManagementRequestDTO.getUserStatus()));
                    }
                    changed = true;
                }

                if (!changed) {
                    return ResponseEntity.ok().body(responseUtil.error(null, 1002,
                            "Login status or user status is required"));
                }

                applicationUser = applicationUserRepository.saveAndFlush(applicationUser);
                ApplicationUserResponseDTO responseDTO = buildEmployeeResponse(applicationUser);
                return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_UPDATE_SUCCESSFULLY, null, locale)));
            }).orElseGet(() -> {
                log.info("Employee user not found {}", employeeManagementRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1043,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND,
                                new Object[]{employeeManagementRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> staffCategoryUpdate(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale) {
        return updateStaffCategory(employeeManagementRequestDTO, locale, false);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> staffCategoryTransfer(EmployeeManagementRequestDTO employeeManagementRequestDTO, Locale locale) {
        return updateStaffCategory(employeeManagementRequestDTO, locale, true);
    }

    private ResponseEntity<ApiResponse<Object>> updateStaffCategory(EmployeeManagementRequestDTO employeeManagementRequestDTO,
                                                                    Locale locale,
                                                                    boolean transferOnly) {
        try {
            log.info("Staff category {} request {}", transferOnly ? "transfer" : "update", employeeManagementRequestDTO);

            return applicationUserRepository.findById(employeeManagementRequestDTO.getId()).map(applicationUser -> {
                log.info("Employee details staff category old audit start");
                return staffCategoriesRepository.findByCodeAndStatus(employeeManagementRequestDTO.getStaffCategory(), Status.ACTIVE).map(staffCategories -> {
                    log.info("Employee details staff category old audit end");
                    return insurancePolicyRepository.findByCodeAndStatus(employeeManagementRequestDTO.getPolicy(), Status.ACTIVE).map(in -> {
                        UserCompanyDetails companyDetails = applicationUser.getUserPersonalDetails().getUserCompanyDetails();
                        StaffCategories previousStaffCategory = companyDetails.getStaffCategories();

                        if (transferOnly) {
                            Optional<String> transferValidationError = validateStaffCategoryTransfer(companyDetails, in, staffCategories);
                            if (transferValidationError.isPresent()) {
                                return ResponseEntity.ok().body(responseUtil.error(null, 1002, transferValidationError.get()));
                            }
                        }

                        log.info("Upload supporting document from dependent employeeId={}", employeeManagementRequestDTO.getId());
                        Document t = null;
                        try {
                            t = uploadImage(employeeManagementRequestDTO.getDocuments().getType(), employeeManagementRequestDTO.getDocuments().getFile(), employeeManagementRequestDTO.getDocuments().getFileType(), employeeManagementRequestDTO.getDocuments().getFileName());
                            documentRepository.saveAndFlush(t);
                        } catch (IOException e) {
                            log.error(e);
                            throw new RuntimeException(e);
                        }

                          companyDetails.setStaffCategories(staffCategories);
                          companyDetails.setInsurancePolicy(in);
                          if (transferOnly) {
                              companyDetails.setTransferDate(employeeManagementRequestDTO.getEffectiveDate());
                              companyDetails.setTransferDocs(t);
                          } else {
                              if (companyDetails.getPreviousPermanentDate() == null) {
                                  companyDetails.setPreviousPermanentDate(companyDetails.getPermanentDate());
                              }
                              companyDetails.setPermanentDate(employeeManagementRequestDTO.getEffectiveDate());
                              companyDetails.setPromoDocs(t);
                          }
                          applicationUserRepository.saveAndFlush(applicationUser);
                        notifyAdminTeamOnStaffCategoryTransferAsync(applicationUser,
                                previousStaffCategory,
                                staffCategories,
                                employeeManagementRequestDTO.getEffectiveDate(),
                                employeeManagementRequestDTO.getUsername());
                        ApplicationUserResponseDTO responseDTO = buildEmployeeResponse(applicationUser);
                        return ResponseEntity.ok().body(responseUtil.success((Object) responseDTO,
                                transferOnly ? "Employee staff category transfer successful"
                                        : messageSource.getMessage(ResponseMessageUtil.STAFF_CATEGORY_UPDATE_SUCCESS, null, locale)));

                    }).orElseGet(() -> {
                        log.info("Insurance period policy not found {}", employeeManagementRequestDTO.getPolicy());
                        return ResponseEntity.ok(responseUtil.error(null, 1046,
                                messageSource.getMessage(ResponseMessageUtil.INSURANCE_PERIOD_NOT_FOUND, null, locale)));
                    });

                }).orElseGet(() -> {
                    log.info("Staff category not found {}", employeeManagementRequestDTO.getStaffCategory());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1043, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND, new Object[]{employeeManagementRequestDTO.getId()}, locale)));
                });

            }).orElseGet(() -> {
                log.info("Employee details not found {}", employeeManagementRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1043, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND, new Object[]{employeeManagementRequestDTO.getId()}, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private Optional<String> validateStaffCategoryTransfer(UserCompanyDetails currentCompanyDetails,
                                                           InsurancePolicy requestedPolicy,
                                                           StaffCategories requestedStaffCategory) {
        if (currentCompanyDetails == null || currentCompanyDetails.getStaffCategories() == null) {
            return Optional.of("Current staff category not found for employee.");
        }

        Optional<InsurancePolicyStaffCategoryGroup> currentGroup = findStaffCategoryGroup(
                currentCompanyDetails.getInsurancePolicy(),
                currentCompanyDetails.getStaffCategories()
        );
        Optional<InsurancePolicyStaffCategoryGroup> requestedGroup = findStaffCategoryGroup(
                requestedPolicy,
                requestedStaffCategory
        );

        if (currentGroup.isEmpty() || requestedGroup.isEmpty()) {
            return Optional.of("Staff category group mapping not found. Please check policy staff category group setup.");
        }

        String currentMainCategory = currentGroup.get().getMainCategoryCode();
        String requestedMainCategory = requestedGroup.get().getMainCategoryCode();
        if (!org.springframework.util.StringUtils.hasText(currentMainCategory)
                || !currentMainCategory.equalsIgnoreCase(requestedMainCategory)) {
            return Optional.of("Selected staff category is not in the same main category. Please use promotion.");
        }

        return Optional.empty();
    }

    private Optional<InsurancePolicyStaffCategoryGroup> findStaffCategoryGroup(InsurancePolicy policy,
                                                                               StaffCategories staffCategory) {
        if (staffCategory == null || !org.springframework.util.StringUtils.hasText(staffCategory.getCode())) {
            return Optional.empty();
        }

        if (policy != null && policy.getId() != null) {
            Optional<InsurancePolicyStaffCategoryGroup> policyGroup = insurancePolicyStaffCategoryGroupRepository
                    .findByInsurancePolicy_IdAndStaffCategories_CodeAndStatus(
                            policy.getId(), staffCategory.getCode(), Status.ACTIVE);
            if (policyGroup.isPresent()) {
                return policyGroup;
            }
        }

        return insurancePolicyStaffCategoryGroupRepository
                .findAllByStaffCategories_CodeAndStatus(staffCategory.getCode(), Status.ACTIVE)
                .stream()
                .filter(group -> org.springframework.util.StringUtils.hasText(group.getMainCategoryCode()))
                .findFirst();
    }

    private void notifyAdminTeamOnStaffCategoryTransferAsync(ApplicationUser applicationUser,
                                                             StaffCategories previousStaffCategory,
                                                             StaffCategories newStaffCategory,
                                                             Date effectiveDate,
                                                             String hrUsername) {
        UserPersonalDetails employee = applicationUser != null ? applicationUser.getUserPersonalDetails() : null;
        String employeeCompanyCode = employee != null
                && employee.getUserCompanyDetails() != null
                && employee.getUserCompanyDetails().getCompanyTypes() != null
                ? employee.getUserCompanyDetails().getCompanyTypes().getCode()
                : null;

        List<WebUser> recipients = webUserRepository.findAllByStatus(Status.ACTIVE).stream()
                .filter(user -> user.getUserRole() != null
                        && org.springframework.util.StringUtils.hasText(user.getUserRole().getCode()))
                .filter(user -> STAFF_CATEGORY_TRANSFER_ADMIN_ROLE_CODES.stream()
                        .anyMatch(roleCode -> roleCode.equalsIgnoreCase(user.getUserRole().getCode())))
                .filter(user -> !org.springframework.util.StringUtils.hasText(employeeCompanyCode)
                        || user.getCompanies() == null
                        || user.getCompanies().isEmpty()
                        || user.getCompanies().stream()
                        .anyMatch(company -> Status.ACTIVE.equals(company.getStatus())
                                && employeeCompanyCode.equalsIgnoreCase(company.getCode())))
                .toList();

        initializeStaffCategoryTransferEmailData(employee, previousStaffCategory, newStaffCategory, recipients);

        Runnable emailTask = () -> {
            try {
                emailNotificationService.notifyStaffCategoryTransferred(
                        recipients,
                        employee,
                        previousStaffCategory,
                        newStaffCategory,
                        effectiveDate,
                        hrUsername
                );
            } catch (Exception ex) {
                log.error("Staff category transfer email failed employeeId={}",
                        employee != null ? employee.getId() : null, ex);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(emailTask);
                }
            });
        } else {
            CompletableFuture.runAsync(emailTask);
        }
    }

    private void initializeStaffCategoryTransferEmailData(UserPersonalDetails employee,
                                                          StaffCategories previousStaffCategory,
                                                          StaffCategories newStaffCategory,
                                                          List<WebUser> recipients) {
        if (employee != null) {
            employee.getId();
            employee.getEpfNo();
            employee.getFirstName();
            employee.getLastName();
            employee.getNic();
            if (employee.getUserCompanyDetails() != null) {
                employee.getUserCompanyDetails().getId();
            }
        }
        if (previousStaffCategory != null) {
            previousStaffCategory.getDescription();
        }
        if (newStaffCategory != null) {
            newStaffCategory.getDescription();
        }
        if (recipients != null) {
            recipients.forEach(recipient -> {
                if (recipient != null) {
                    recipient.getEmail();
                    if (recipient.getUserRole() != null) {
                        recipient.getUserRole().getCode();
                    }
                }
            });
        }
    }

    protected Document uploadImage(String tye, String file, String fileType, String fileName) throws IOException {
        try {
            log.info("Upload Document image");
            MultipartFile multipartFile = MultipartFileUtil.convertToMultipartFile(file, fileType, fileName);
            DocumentUploadRequestDTO dto = new DocumentUploadRequestDTO();
            dto.setType(tye);
            dto.setDocument(file.getBytes());
            dto.setFileName(multipartFile.getOriginalFilename());
            dto.setFileType(multipartFile.getContentType());
            Document document = gson.fromJson(gson.toJson(dto), Document.class);
            document.setDoc(file);
            document = documentRepository.saveAndFlush(document);
            log.info("image upload success id={} type={} fileName={} fileType={}",
                    document.getId(), document.getType(), document.getFileName(), document.getFileType());
            return document;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private ApplicationUserResponseDTO buildEmployeeResponse(ApplicationUser applicationUser) {
        ApplicationUserResponseDTO applicationUserResponseDTO = employeeUserMapperEntityToDto.mapEmployeeUserDetails(applicationUser);

        if (applicationUser.getUserPersonalDetails() != null) {
            populateRejoinDetails(applicationUserResponseDTO, applicationUser.getUserPersonalDetails());

            if (applicationUser.getUserPersonalDetails().getBirthImg() != null) {
                applicationUserResponseDTO.getUserPersonalDetails().setBirthImg(
                        mapDocument(applicationUser.getUserPersonalDetails().getBirthImg()));
            }

            if (applicationUser.getUserPersonalDetails().getMaritalDetails() != null
                    && applicationUser.getUserPersonalDetails().getMaritalDetails().getDocuments() != null
                    && !applicationUser.getUserPersonalDetails().getMaritalDetails().getDocuments().isEmpty()) {
                applicationUserResponseDTO.getUserPersonalDetails().setMaritalStatusDocument(
                        mapDocument(applicationUser.getUserPersonalDetails().getMaritalDetails().getDocuments().getFirst()));
            }

            if (applicationUser.getUserPersonalDetails().getUserCompanyDetails() != null
                    && applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPromoDocs() != null) {
                UserCompanyDetails companyDetails = applicationUser.getUserPersonalDetails().getUserCompanyDetails();
                DocumentDownloadResponseDTO promoDoc = mapDocument(
                        companyDetails.getPromoDocs());
                applicationUserResponseDTO.getUserPersonalDetails().getUserCompanyDetails().setPromoDoc(promoDoc);
                applicationUserResponseDTO.getUserPersonalDetails().setPromoDoc(promoDoc);
            }

            if (applicationUser.getUserPersonalDetails().getUserCompanyDetails() != null) {
                UserCompanyDetails companyDetails = applicationUser.getUserPersonalDetails().getUserCompanyDetails();
                DocumentDownloadResponseDTO transferDoc = mapDocument(companyDetails.getTransferDocs());
                applicationUserResponseDTO.getUserPersonalDetails().getUserCompanyDetails()
                        .setTransferDate(companyDetails.getTransferDate());
                applicationUserResponseDTO.getUserPersonalDetails().getUserCompanyDetails()
                        .setTransferDoc(transferDoc);
                applicationUserResponseDTO.getUserPersonalDetails().setTransferDate(companyDetails.getTransferDate());
                applicationUserResponseDTO.getUserPersonalDetails().setTransferDoc(transferDoc);
            }
        }

        return applicationUserResponseDTO;
    }

    private void populateRejoinDetails(ApplicationUserResponseDTO responseDTO, UserPersonalDetails currentUser) {
        if (responseDTO == null || currentUser == null || !org.springframework.util.StringUtils.hasText(currentUser.getNic())
                || currentUser.getId() == null) {
            return;
        }

        List<UserPersonalDetails> previousProfiles = userPersonalDetailsRepository
                .findAllByNicIgnoreCaseAndUserStatusAndIdNotOrderByIdDesc(currentUser.getNic(), Status.INACTIVE, currentUser.getId());

        if (previousProfiles.isEmpty()) {
            return;
        }

        String currentEpf = normalizeValue(currentUser.getEpfNo());
        LinkedHashMap<String, SimpleBaseDTO> previousCompanies = new LinkedHashMap<>();
        LinkedHashSet<String> previousEpfs = new LinkedHashSet<>();

        previousProfiles.forEach(previousProfile -> {
            String previousEpf = normalizeValue(previousProfile.getEpfNo());
            if (previousEpf != null && !previousEpf.equalsIgnoreCase(currentEpf)) {
                previousEpfs.add(previousEpf);
            }

            CompanyTypes companyTypes = previousProfile.getUserCompanyDetails() != null
                    ? previousProfile.getUserCompanyDetails().getCompanyTypes()
                    : null;
            if (companyTypes != null && org.springframework.util.StringUtils.hasText(companyTypes.getCode())) {
                previousCompanies.putIfAbsent(companyTypes.getCode(),
                        new SimpleBaseDTO(companyTypes.getCode(), companyTypes.getDescription()));
            }
        });

        if (previousCompanies.isEmpty() && previousEpfs.isEmpty()) {
            return;
        }

        EmployeeRejoinDetailsResponseDTO rejoinDetails = new EmployeeRejoinDetailsResponseDTO();
        rejoinDetails.setPreviousCompanies(new ArrayList<>(previousCompanies.values()));
        rejoinDetails.setPreviousEpfs(new ArrayList<>(previousEpfs));
        responseDTO.setRejoinDetails(rejoinDetails);
    }

    private String normalizeValue(String value) {
        return org.springframework.util.StringUtils.hasText(value) ? value.trim() : null;
    }

    private DocumentDownloadResponseDTO mapDocument(Document document) {
        if (document == null) {
            return null;
        }
        DocumentDownloadResponseDTO dto = new DocumentDownloadResponseDTO();
        dto.setType(document.getType() != null ? document.getType().name() : null);
        dto.setFileName(document.getFileName());
        dto.setFileType(document.getFileType());
        dto.setDoc(document.getDoc());
        return dto;
    }

}

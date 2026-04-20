package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.request.UserCompanyDetailsRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.DocumentDownloadResponseDTO;
import com.dtech.admin.dto.response.EmployeeDetailsResponseDTO;
import com.dtech.admin.dto.response.EmployeeRejoinDetailsResponseDTO;
import com.dtech.admin.dto.search.EmployeeSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.enums.MaritalStatus;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.audit.EmployeeDetailsAuditMapper;
import com.dtech.admin.mapper.dtoToEntity.EmployeeDetailsMapperDtoToEntity;
import com.dtech.admin.mapper.entityToDto.EmployeeDetailsMapperEntityToDto;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.EmailNotificationService;
import com.dtech.admin.service.EmployeeService;
import com.dtech.admin.specifications.EmployeeSpecification;
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
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final Set<String> EMPLOYEE_INCLUSION_ADMIN_ROLE_CODES = Set.of(
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
    private final CompanyTypeRepository companyTypeRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final StaffTypesRepository staffTypesRepository;

    @Autowired
    private final InsurancePolicyRepository insurancePolicyRepository;

    @Autowired
    private final UserPersonalDetailsRepository userPersonalDetailsRepository;

    @Autowired
    private final WebUserRepository webUserRepository;

    @Autowired
    private final EmployeeDetailsMapperEntityToDto employeeDetailsMapperEntityToDto;

    @Autowired
    private final EmployeeDetailsMapperDtoToEntity employeeDetailsMapperDtoToEntity;

    @Autowired
    private final EmployeeDetailsAuditMapper employeeDetailsAuditMapper;

    @Autowired
    private final EmailNotificationService emailNotificationService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Employee details ref data {}", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.EMPM.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Status.values())
                    .filter(status -> !Status.DELETE.name().equals(status.name()))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> title = Arrays.stream(Title.values())
                    .filter(st -> !Title.MS.name().equals(st.name()))
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
            responseMap.put("title", title);
            auditLogService.log(WebPage.EMPM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.EMPM.name()}, locale)));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<EmployeeSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Employee details filter data {}", paginationRequest);
            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            Set<String> eligibleCompanyCodes = getEligibleCompanyCodes(paginationRequest.getUsername());

            Page<UserPersonalDetails> userPersonalDetails = Objects.nonNull(paginationRequest.getSearch()) ?
                    userPersonalDetailsRepository.findAll(EmployeeSpecification.getSpecification(paginationRequest.getSearch(), eligibleCompanyCodes), pageable) :
                    userPersonalDetailsRepository.findAll(EmployeeSpecification.getSpecification(eligibleCompanyCodes), pageable);
            log.info("Employee details filter records {}", userPersonalDetails);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    userPersonalDetailsRepository.count(EmployeeSpecification.getSpecification(paginationRequest.getSearch(), eligibleCompanyCodes)) :
                    userPersonalDetailsRepository.count(EmployeeSpecification.getSpecification(eligibleCompanyCodes));
            log.info("Employee details filter records map start");
            List<EmployeeDetailsResponseDTO> responseDTOList = userPersonalDetails.stream()
                    .map(employeeDetailsMapperEntityToDto::mapEmployeeDetails).toList();
            log.info("Employee details filter records map finish");
            List<String> newAuditList = employeeDetailsAuditMapper.mapToDTOAudit(userPersonalDetails.stream().toList());
            auditLogService.log(WebPage.EMPM.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<EmployeeDetailsResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_FILTER_LIST_SUCCESSFULLY,
                            null, locale)));
        } catch (Exception e) {
            log.error(e);
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
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(() -> companyTypeRepository.findAllByStatus(Status.ACTIVE).stream()
                        .map(company -> company.getCode())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> add(EmployeeDetailsRequestDTO dto, Locale locale) {
        try {
            log.info("Adding employee: {}", dto);

            if (userPersonalDetailsRepository.existsByNicIgnoreCaseAndUserStatusIn(dto.getNic(), List.of(Status.ACTIVE))) {
                return errorResponse(1041, ResponseMessageUtil.EMPLOYEE_NIC_ALREADY_EXISTED, dto.getNic(), locale);
            }

            if (userPersonalDetailsRepository.existsByEmailIgnoreCaseAndUserStatusIn(dto.getEmail(), List.of(Status.ACTIVE))) {
                return errorResponse(1042, ResponseMessageUtil.EMPLOYEE_EMAIL_ALREADY_EXISTED, dto.getEmail(), locale);
            }

            Optional<CompanyTypes> companyOpt = companyTypeRepository.findByCodeAndStatus(dto.getUserCompanyDetails().getCompanyTypeCode(), Status.ACTIVE);
            if (companyOpt.isEmpty()) {
                return errorResponse(1036, ResponseMessageUtil.COMPANY_NOT_FOUND, dto.getUserCompanyDetails().getCompanyTypeCode(), locale);
            }

            String paymentCompanyCode = resolvePaymentCompanyCode(dto.getUserCompanyDetails());
            String deathPaymentCompanyCode = resolveDeathPaymentCompanyCode(dto.getUserCompanyDetails());
            String insurancePolicyCode = resolveInsurancePolicyCode(dto.getUserCompanyDetails());
            Optional<CompanyTypes> paymentCompanyOpt = Optional.empty();
            if (StringUtils.hasText(paymentCompanyCode)) {
                paymentCompanyOpt = companyTypeRepository.findByCodeAndStatus(paymentCompanyCode, Status.ACTIVE);
                if (paymentCompanyOpt.isEmpty()) {
                    return errorResponse(1036, ResponseMessageUtil.COMPANY_NOT_FOUND, paymentCompanyCode, locale);
                }
            }
            Optional<CompanyTypes> deathPaymentCompanyOpt = Optional.empty();
            if (StringUtils.hasText(deathPaymentCompanyCode)) {
                deathPaymentCompanyOpt = companyTypeRepository.findByCodeAndStatus(deathPaymentCompanyCode, Status.ACTIVE);
                if (deathPaymentCompanyOpt.isEmpty()) {
                    return errorResponse(1036, ResponseMessageUtil.COMPANY_NOT_FOUND, deathPaymentCompanyCode, locale);
                }
            }

            if (userPersonalDetailsRepository.existsByEpfNoIgnoreCaseAndUserStatusInAndUserCompanyDetails_companyTypes_code(dto.getEpfNo(), List.of(Status.ACTIVE, Status.INACTIVE),companyOpt.get().getCode())) {
                return errorResponse(1040, ResponseMessageUtil.EMPLOYEE_EPF_ALREADY_EXISTED, dto.getEpfNo(), locale);
            }

            Optional<StaffCategories> staffCategoryOpt = staffCategoriesRepository.findByCodeAndStatus(dto.getUserCompanyDetails().getStaffCategoryCode(), Status.ACTIVE);
            if (staffCategoryOpt.isEmpty()) {
                return errorResponse(1037, ResponseMessageUtil.STAFF_CATEGORY_NOT_FOUND, dto.getUserCompanyDetails().getStaffCategoryCode(), locale);
            }

            Optional<StaffTypes> staffTypeOpt = staffTypesRepository.findByCodeAndStatus(dto.getUserCompanyDetails().getStaffTypeCode(), Status.ACTIVE);
            if (staffTypeOpt.isEmpty()) {
                return errorResponse(1038, ResponseMessageUtil.STAFF_TYPE_NOT_FOUND, dto.getUserCompanyDetails().getStaffTypeCode(), locale);
            }

            Optional<InsurancePolicy> insurancePolicy = Optional.empty();
            if (StringUtils.hasText(insurancePolicyCode)) {
                insurancePolicy = insurancePolicyRepository.findByCodeAndStatus(
                        insurancePolicyCode, Status.ACTIVE);
                if (insurancePolicy.isEmpty()) {
                    return errorResponse(1039, ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND,
                            insurancePolicyCode, locale);
                }
            }

            UserCompanyDetails companyDetails = employeeDetailsMapperDtoToEntity.mapCompanyDetails(dto.getUserCompanyDetails());
            companyDetails.setCompanyTypes(companyOpt.get());
            companyDetails.setPaymentCompany(paymentCompanyOpt.orElse(null));
            companyDetails.setDeathPaymentCompany(deathPaymentCompanyOpt.orElse(null));
            companyDetails.setStaffTypes(staffTypeOpt.get());
            companyDetails.setStaffCategories(staffCategoryOpt.get());
            companyDetails.setInsurancePolicy(insurancePolicy.orElse(null));
            companyDetails.setFacility(Facility.valueOf(dto.getUserCompanyDetails().getFacility()));

            UserPersonalDetails personalDetails = employeeDetailsMapperDtoToEntity.mapPersonalDetails(dto);
            personalDetails.setGender(Gender.valueOf(dto.getGender()));
            personalDetails.setTitle(Title.valueOf(dto.getTitle()));
            personalDetails.setUserStatus(Status.valueOf(dto.getUserStatus()));
            personalDetails.setUserCompanyDetails(companyDetails);
            personalDetails.setIsTemp(false);

            userPersonalDetailsRepository.saveAndFlush(personalDetails);

            List<String> auditList = employeeDetailsAuditMapper.mapToDTOAudit(List.of(personalDetails));
            auditLogService.log(WebPage.EMPM.name(), WebTask.ADD.name(),
                    AuditTask.ADD_DATA.getDescription(), dto.getIp(), dto.getUserAgent(),
                    gson.toJson(auditList), null, dto.getUsername());

            notifyAdminTeamOnEmployeeAddition(personalDetails, dto.getUsername());

            return ResponseEntity.ok().body(responseUtil.success(null,
                    messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_ADDED_SUCCESSFULLY, new Object[]{personalDetails.getEpfNo()}, locale)));

        } catch (Exception e) {
            log.error("Error occurred while adding employee", e);
            return ResponseEntity.internalServerError().body(responseUtil.error(null, 500, "Internal Server Error"));
        }
    }

    private ResponseEntity<ApiResponse<Object>> errorResponse(int code, String messageKey, String arg, Locale locale) {
        String message = messageSource.getMessage(messageKey, new Object[]{arg}, locale);
        return ResponseEntity.ok().body(responseUtil.error(null, code, message));
    }

    private List<WebUser> resolveEmployeeAdminRecipients(UserPersonalDetails employee) {
        String employeeCompanyCode = employee.getUserCompanyDetails() != null
                && employee.getUserCompanyDetails().getCompanyTypes() != null
                ? employee.getUserCompanyDetails().getCompanyTypes().getCode()
                : null;

        return webUserRepository.findAllByStatus(Status.ACTIVE).stream()
                .filter(user -> user.getUserRole() != null && StringUtils.hasText(user.getUserRole().getCode()))
                .filter(user -> EMPLOYEE_INCLUSION_ADMIN_ROLE_CODES.stream()
                        .anyMatch(roleCode -> roleCode.equalsIgnoreCase(user.getUserRole().getCode())))
                .filter(user -> !StringUtils.hasText(employeeCompanyCode)
                        || user.getCompanies() == null
                        || user.getCompanies().isEmpty()
                        || user.getCompanies().stream()
                        .anyMatch(company -> Status.ACTIVE.equals(company.getStatus())
                                && employeeCompanyCode.equalsIgnoreCase(company.getCode())))
                .toList();
    }

    private void notifyAdminTeamOnEmployeeAddition(UserPersonalDetails employee, String hrUsername) {
        emailNotificationService.notifyEmployeeAddedPendingApproval(resolveEmployeeAdminRecipients(employee), employee, hrUsername);
    }

    private void notifyAdminTeamOnEmployeeDeactivation(UserPersonalDetails employee, String hrUsername) {
        emailNotificationService.notifyEmployeeDeactivated(resolveEmployeeAdminRecipients(employee), employee, hrUsername);
    }

    private String resolvePaymentCompanyCode(UserCompanyDetailsRequestDTO userCompanyDetails) {
        if (userCompanyDetails == null) {
            return null;
        }
        String code = userCompanyDetails.getPaymentCompanyCode();
        if (!StringUtils.hasText(code) && userCompanyDetails.getPaymentCompany() != null) {
            code = userCompanyDetails.getPaymentCompany().getCode();
        }
        return StringUtils.hasText(code) ? code.trim() : null;
    }

    private String resolveDeathPaymentCompanyCode(UserCompanyDetailsRequestDTO userCompanyDetails) {
        if (userCompanyDetails == null) {
            return null;
        }
        String code = userCompanyDetails.getDeathPaymentCompanyCode();
        if (!StringUtils.hasText(code) && userCompanyDetails.getDeathPaymentCompany() != null) {
            code = userCompanyDetails.getDeathPaymentCompany().getCode();
        }
        return StringUtils.hasText(code) ? code.trim() : null;
    }

    private String resolveInsurancePolicyCode(UserCompanyDetailsRequestDTO userCompanyDetails) {
        if (userCompanyDetails == null) {
            return null;
        }
        String code = userCompanyDetails.getInsurancePolicyCode();
        if (!StringUtils.hasText(code) && userCompanyDetails.getInsurancePolicy() != null) {
            code = userCompanyDetails.getInsurancePolicy().getCode();
        }
        return StringUtils.hasText(code) ? code.trim() : null;
    }


    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(EmployeeDetailsRequestDTO employeeDetailsRequestDTO, Locale locale) {
        try {
            log.info("Employee details view {}", employeeDetailsRequestDTO);
            return userPersonalDetailsRepository.findById(employeeDetailsRequestDTO.getId()).map(userPersonalDetails -> {
                EmployeeDetailsResponseDTO employeeDetailsResponseDTO = employeeDetailsMapperEntityToDto.mapEmployeeDetails(userPersonalDetails);
                populateRejoinDetails(employeeDetailsResponseDTO, userPersonalDetails);
                List<String> newAuditList = employeeDetailsAuditMapper.mapToDTOAudit(List.of(userPersonalDetails));
                auditLogService.log(WebPage.EMPM.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), employeeDetailsRequestDTO.getIp(), employeeDetailsRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, employeeDetailsRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) employeeDetailsResponseDTO, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_RETRIEVE_SUCCESSFULLY, null, locale)));

            }).orElseGet(() -> {
                log.info("Employee details not found {}", employeeDetailsRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1043, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND, new Object[]{employeeDetailsRequestDTO.getId()}, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private void populateRejoinDetails(EmployeeDetailsResponseDTO responseDTO, UserPersonalDetails currentUser) {
        if (responseDTO == null || currentUser == null || !StringUtils.hasText(currentUser.getNic()) || currentUser.getId() == null) {
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
            if (companyTypes != null && StringUtils.hasText(companyTypes.getCode())) {
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
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(EmployeeDetailsRequestDTO employeeDetailsRequestDTO, Locale locale) {
        try {
            log.info("Employee details update {}", employeeDetailsRequestDTO.getId());
            return userPersonalDetailsRepository.findById(employeeDetailsRequestDTO.getId()).map(userPersonalDetails -> {
                Status previousStatus = userPersonalDetails.getUserStatus();
                String requestedPaymentCompanyCode = resolvePaymentCompanyCode(employeeDetailsRequestDTO.getUserCompanyDetails());
                String requestedDeathPaymentCompanyCode = resolveDeathPaymentCompanyCode(employeeDetailsRequestDTO.getUserCompanyDetails());
                String existingPaymentCompanyCode = userPersonalDetails.getUserCompanyDetails().getPaymentCompany() != null
                        ? userPersonalDetails.getUserCompanyDetails().getPaymentCompany().getCode()
                        : null;
                String effectivePaymentCompanyCode = StringUtils.hasText(requestedPaymentCompanyCode)
                        ? requestedPaymentCompanyCode
                        : existingPaymentCompanyCode;
                String existingDeathPaymentCompanyCode = userPersonalDetails.getUserCompanyDetails().getDeathPaymentCompany() != null
                        ? userPersonalDetails.getUserCompanyDetails().getDeathPaymentCompany().getCode()
                        : null;
                String effectiveDeathPaymentCompanyCode = StringUtils.hasText(requestedDeathPaymentCompanyCode)
                        ? requestedDeathPaymentCompanyCode
                        : existingDeathPaymentCompanyCode;
                String requestedInsurancePolicyCode = resolveInsurancePolicyCode(employeeDetailsRequestDTO.getUserCompanyDetails());
                String existingInsurancePolicyCode = userPersonalDetails.getUserCompanyDetails().getInsurancePolicy() != null
                        ? userPersonalDetails.getUserCompanyDetails().getInsurancePolicy().getCode()
                        : null;
                String effectiveInsurancePolicyCode = StringUtils.hasText(requestedInsurancePolicyCode)
                        ? requestedInsurancePolicyCode
                        : existingInsurancePolicyCode;
                Date effectiveDob = Objects.nonNull(employeeDetailsRequestDTO.getDob())
                        ? employeeDetailsRequestDTO.getDob()
                        : userPersonalDetails.getDob();

                String newModel = new StringBuilder()
                        .append(employeeDetailsRequestDTO.getNic())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getEmail())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getMobileNo())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getInitials())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getFirstName())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getLastName())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getUserStatus())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getTitle())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getMaritalStatus())
                        .append("|")
                        .append(effectiveDob)
                        .append("|")
                        .append(employeeDetailsRequestDTO.getUserAddress().getCity())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getUserAddress().getStreet1())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getUserAddress().getStreet2())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getUserAddress().getStreetNo())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getUserCompanyDetails().getDesignation())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getUserCompanyDetails().getFacility())
                        .append("|")
                        .append(employeeDetailsRequestDTO.getUserCompanyDetails().getTerminateDate())
                        .append("|")
                        .append(effectivePaymentCompanyCode)
                        .append("|")
                        .append(effectiveDeathPaymentCompanyCode)
                        .append("|")
                        .append(effectiveInsurancePolicyCode).toString();

                String oldModel = new StringBuilder()
                        .append(userPersonalDetails.getNic())
                        .append("|")
                        .append(userPersonalDetails.getEmail())
                        .append("|")
                        .append(userPersonalDetails.getMobileNo())
                        .append("|")
                        .append(userPersonalDetails.getInitials())
                        .append("|")
                        .append(userPersonalDetails.getFirstName())
                        .append("|")
                        .append(userPersonalDetails.getLastName())
                        .append("|")
                        .append(userPersonalDetails.getUserStatus().name())
                        .append("|")
                        .append(userPersonalDetails.getTitle().name())
                        .append("|")
                        .append(userPersonalDetails.getMaritalStatus())
                        .append("|")
                        .append(userPersonalDetails.getDob())
                        .append("|")
                        .append(userPersonalDetails.getUserAddress().getCity())
                        .append("|")
                        .append(userPersonalDetails.getUserAddress().getStreet1())
                        .append("|")
                        .append(userPersonalDetails.getUserAddress().getStreet2())
                        .append("|")
                        .append(userPersonalDetails.getUserAddress().getStreetNo())
                        .append("|")
                        .append(userPersonalDetails.getUserCompanyDetails().getDesignation())
                        .append("|")
                        .append(userPersonalDetails.getUserCompanyDetails().getFacility().name())
                        .append("|")
                        .append(userPersonalDetails.getUserCompanyDetails().getTerminateDate())
                        .append("|")
                        .append(existingPaymentCompanyCode)
                        .append("|")
                        .append(existingDeathPaymentCompanyCode)
                        .append("|")
                        .append(existingInsurancePolicyCode).toString();

                if (oldModel.equals(newModel)) {
                    log.info("Employee details update data not changed to {}", newModel);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1044, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_CHANGING, null, locale)));
                }

                log.info("Employee details update old audit start");
                List<String> oldAuditList = employeeDetailsAuditMapper.mapToDTOAudit(List.of(userPersonalDetails));
                log.info("Employee details update old audit end");

                userPersonalDetails.setNic(employeeDetailsRequestDTO.getNic());
                userPersonalDetails.setUserStatus(Status.valueOf(employeeDetailsRequestDTO.getUserStatus()));
                userPersonalDetails.setEmail(employeeDetailsRequestDTO.getEmail());
                userPersonalDetails.setMobileNo(employeeDetailsRequestDTO.getMobileNo());
                userPersonalDetails.setInitials(employeeDetailsRequestDTO.getInitials());
                userPersonalDetails.setFirstName(employeeDetailsRequestDTO.getFirstName());
                userPersonalDetails.setLastName(employeeDetailsRequestDTO.getLastName());
                userPersonalDetails.setTitle(Title.valueOf(employeeDetailsRequestDTO.getTitle()));
                userPersonalDetails.setMaritalStatus(MaritalStatus.valueOf(employeeDetailsRequestDTO.getMaritalStatus()));
                if (Objects.nonNull(employeeDetailsRequestDTO.getDob())) {
                    userPersonalDetails.setDob(employeeDetailsRequestDTO.getDob());
                }
                userPersonalDetails.getUserAddress().setCity(employeeDetailsRequestDTO.getUserAddress().getCity());
                userPersonalDetails.getUserAddress().setStreet1(employeeDetailsRequestDTO.getUserAddress().getStreet1());
                userPersonalDetails.getUserAddress().setStreet2(Objects.nonNull(employeeDetailsRequestDTO.getUserAddress().getStreet2()) ? employeeDetailsRequestDTO.getUserAddress().getStreet2() : null);
                userPersonalDetails.getUserAddress().setStreetNo(employeeDetailsRequestDTO.getUserAddress().getStreetNo());
                userPersonalDetails.getUserCompanyDetails().setDesignation(employeeDetailsRequestDTO.getUserCompanyDetails().getDesignation());
                userPersonalDetails.getUserCompanyDetails().setFacility(Facility.valueOf(employeeDetailsRequestDTO.getUserCompanyDetails().getFacility()));
                userPersonalDetails.getUserCompanyDetails().setTerminateDate(Objects.nonNull(employeeDetailsRequestDTO.getUserCompanyDetails().getTerminateDate()) ? employeeDetailsRequestDTO.getUserCompanyDetails().getTerminateDate() : null);
                if (StringUtils.hasText(requestedInsurancePolicyCode)) {
                    Optional<InsurancePolicy> insurancePolicyOpt = insurancePolicyRepository.findByCodeAndStatus(
                            requestedInsurancePolicyCode, Status.ACTIVE);
                    if (insurancePolicyOpt.isEmpty()) {
                        return errorResponse(1039, ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND,
                                requestedInsurancePolicyCode, locale);
                    }
                    userPersonalDetails.getUserCompanyDetails().setInsurancePolicy(insurancePolicyOpt.get());
                } else if (!Facility.DEATH.name().equalsIgnoreCase(employeeDetailsRequestDTO.getUserCompanyDetails().getFacility())) {
                    return errorResponse(1039, ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND, "Required", locale);
                } else {
                    userPersonalDetails.getUserCompanyDetails().setInsurancePolicy(null);
                }
                if (StringUtils.hasText(requestedPaymentCompanyCode)) {
                    Optional<CompanyTypes> paymentCompanyOpt = companyTypeRepository.findByCodeAndStatus(
                            requestedPaymentCompanyCode, Status.ACTIVE);
                    if (paymentCompanyOpt.isEmpty()) {
                        return errorResponse(1036, ResponseMessageUtil.COMPANY_NOT_FOUND, requestedPaymentCompanyCode, locale);
                    }
                    userPersonalDetails.getUserCompanyDetails().setPaymentCompany(paymentCompanyOpt.get());
                }
                if (StringUtils.hasText(requestedDeathPaymentCompanyCode)) {
                    Optional<CompanyTypes> deathPaymentCompanyOpt = companyTypeRepository.findByCodeAndStatus(
                            requestedDeathPaymentCompanyCode, Status.ACTIVE);
                    if (deathPaymentCompanyOpt.isEmpty()) {
                        return errorResponse(1036, ResponseMessageUtil.COMPANY_NOT_FOUND, requestedDeathPaymentCompanyCode, locale);
                    }
                    userPersonalDetails.getUserCompanyDetails().setDeathPaymentCompany(deathPaymentCompanyOpt.get());
                }
                log.info("Employee details success");
                userPersonalDetails = userPersonalDetailsRepository.saveAndFlush(userPersonalDetails);

                if (!Status.INACTIVE.equals(previousStatus) && Status.INACTIVE.equals(userPersonalDetails.getUserStatus())) {
                    notifyAdminTeamOnEmployeeDeactivation(userPersonalDetails, employeeDetailsRequestDTO.getUsername());
                }

                List<String> newAuditList = employeeDetailsAuditMapper.mapToDTOAudit(List.of(userPersonalDetails));
                auditLogService.log(WebPage.EMPM.name(), WebTask.UPDATE.name(), AuditTask.UPDATE_DATA.getDescription(), employeeDetailsRequestDTO.getIp(), employeeDetailsRequestDTO.getUserAgent(), gson.toJson(newAuditList), gson.toJson(oldAuditList), employeeDetailsRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_UPDATE_SUCCESSFULLY, null, locale)));

            }).orElseGet(() -> {
                log.info("Employee details not found {}", employeeDetailsRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1043, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND, new Object[]{employeeDetailsRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> delete(EmployeeDetailsRequestDTO employeeDetailsRequestDTO, Locale locale) {
        try {
            log.info("Employee details delete {}", employeeDetailsRequestDTO);

            return userPersonalDetailsRepository.findById(employeeDetailsRequestDTO.getId()).map(userPersonalDetails -> {
                log.info("Employee details delete old audit start");
                List<String> oldAuditList = employeeDetailsAuditMapper.mapToDTOAudit(List.of(userPersonalDetails));
                log.info("Employee details delete old audit end");
                userPersonalDetails.setUserStatus(Status.DELETE);
                userPersonalDetailsRepository.saveAndFlush(userPersonalDetails);
                List<String> newAuditList = employeeDetailsAuditMapper.mapToDTOAudit(List.of(userPersonalDetails));
                auditLogService.log(WebPage.EMPM.name(), WebTask.DELETE.name(), AuditTask.DELETE_DATA.getDescription(), employeeDetailsRequestDTO.getIp(), employeeDetailsRequestDTO.getUserAgent(), gson.toJson(newAuditList), gson.toJson(oldAuditList), employeeDetailsRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_DELETE_SUCCESSFULLY, null, locale)));

            }).orElseGet(() -> {
                log.info("Employee details not found {}", employeeDetailsRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1043, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND, new Object[]{employeeDetailsRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}

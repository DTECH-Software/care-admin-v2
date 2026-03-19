package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.CivilStatusApprovalRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.MaritalStatusApprovalResponseDTO;
import com.dtech.admin.dto.search.CivilStatusChangeSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.mapper.entityToDto.CivilStatusChangeStatusApprovalEntityToDto;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.ApplicationUserRepository;
import com.dtech.admin.repository.CompanyTypeRepository;
import com.dtech.admin.repository.MaritalStatusRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.repository.WebUserRepository;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.EmailNotificationService;
import com.dtech.admin.service.EmployeeCivilStatusApprovalService;
import com.dtech.admin.service.MessageService;
import com.dtech.admin.specifications.MaritalSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
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
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmployeeCivilStatusApprovalServiceImpl implements EmployeeCivilStatusApprovalService {

    private static final Set<String> CIVIL_STATUS_APPROVAL_ADMIN_ROLE_CODES = Set.of(
            "DevTest", "SUPERADMIN", "APPROVER", "ADMIN", "CLAIMS_APPROVER", "W_CSA"
    );

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
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final MaritalStatusRepository maritalStatusRepository;

    @Autowired
    private final CivilStatusChangeStatusApprovalEntityToDto civilStatusChangeStatusApprovalEntityToDto;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;
    @Autowired
    private CompanyTypeRepository companyTypeRepository;
    @Autowired
    private WebUserRepository webUserRepository;

    @Autowired
    private final EmailNotificationService emailNotificationService;

    @Autowired
    private final MessageService messageService;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {

            log.info("Claims approval claims request {} ", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.CSAM.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Workflow.values())
                    .filter(status -> !Status.ACTIVE.name().equals(status.name()))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> civilStatus = Arrays.stream(MaritalStatus.values())
                    .filter(status -> !MaritalStatus.DIVORCE.name().equals(status.name()))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> staffCategory = staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> company = getEligibleCompanies(channelRequestDTO.getUsername());

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            responseMap.put("staffCategory", staffCategory);
            responseMap.put("civilStatus", civilStatus);
            responseMap.put("company", company);

            auditLogService.log(WebPage.CSAM.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.CSAM.name()}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<CivilStatusChangeSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Claims approval claims request {} ", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            Set<String> eligibleCompanyCodes = getEligibleCompanyCodes(paginationRequest.getUsername());

            Page<com.dtech.admin.model.MaritalStatus> maritalStatuses = Objects.nonNull(paginationRequest.getSearch()) ?
                    maritalStatusRepository.findAll(MaritalSpecification.getSpecification(paginationRequest.getSearch(), eligibleCompanyCodes), pageable) :
                    maritalStatusRepository.findAll(MaritalSpecification.getSpecification(eligibleCompanyCodes), pageable);
            log.info("Approval death details filter records {}", maritalStatuses);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    maritalStatusRepository.count(MaritalSpecification.getSpecification(paginationRequest.getSearch(), eligibleCompanyCodes)) :
                    maritalStatusRepository.count(MaritalSpecification.getSpecification(eligibleCompanyCodes));
            log.info("Approval death details filter records map start");

            List<MaritalStatusApprovalResponseDTO> responseDTOList = maritalStatuses.stream()
                    .map(civilStatusChangeStatusApprovalEntityToDto::mapCivilStatusApproval).toList();

            log.info("Approval death details filter records map finish");
            //         List<String> newAuditList = deathApprovalAuditMapper.mapToDTOAudit(deathClaimRequests.stream().toList());
            //        auditLogService.log(WebPage.DDFA.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<MaritalStatusApprovalResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.CIVIL_STATUS_DETAILS_FILTER_LIST_SUCCESSFULLY,
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
    public ResponseEntity<ApiResponse<Object>> imageRequest(CivilStatusApprovalRequestDTO civilStatusApprovalRequestDTO, Locale locale) {
        try {
            log.info("Image request {} ", civilStatusApprovalRequestDTO);

            return maritalStatusRepository.findById(civilStatusApprovalRequestDTO.getId()).map(maritalStatus -> {

                MaritalStatusApprovalResponseDTO maritalStatusApprovalResponseDTO = civilStatusChangeStatusApprovalEntityToDto
                        .mapCivilStatusApproval(maritalStatus);

                return ResponseEntity.ok().body(responseUtil.success((Object) maritalStatusApprovalResponseDTO, messageSource.getMessage(ResponseMessageUtil.CIVIL_STATUS_DOCUMENT_DOWNLOAD_SUCCESSFULLY, null, locale)));

            }).orElseGet(() -> {
                log.info("Image request {} not found", civilStatusApprovalRequestDTO);
                return ResponseEntity.ok(responseUtil.error(null, 1011,
                        messageSource.getMessage(ResponseMessageUtil.CIVIL_STATUS_NOT_FOUND, new Object[]{civilStatusApprovalRequestDTO.getId()}, locale)));

            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> updateStatus(CivilStatusApprovalRequestDTO civilStatusApprovalRequestDTO, Locale locale) {
       try {
           log.info("Update status request {} ", civilStatusApprovalRequestDTO);

           return maritalStatusRepository.findById(civilStatusApprovalRequestDTO.getId()).map(maritalStatus -> {
               Workflow previousStatus = maritalStatus.getStatus();

               maritalStatus.setStatus(Workflow.valueOf(civilStatusApprovalRequestDTO.getStatus()));
               maritalStatusRepository.saveAndFlush(maritalStatus);
               maritalStatus.getApplicationUser().getUserPersonalDetails().setMaritalStatus(maritalStatus.getMaritalStatus());
               applicationUserRepository.saveAndFlush(maritalStatus.getApplicationUser());

               if (!Workflow.APPROVED.equals(previousStatus) && Workflow.APPROVED.equals(maritalStatus.getStatus())) {
                   notifyAdminTeamOnCivilStatusApproval(maritalStatus, civilStatusApprovalRequestDTO.getUsername());
               } else if (Workflow.REJECTED.equals(maritalStatus.getStatus())) {
                   notifyEmployeeOnCivilStatusRejection(maritalStatus);
               }
               return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.CIVIL_STATUS_UPDATE_SUCCESSFULLY, null, locale)));

           }).orElseGet(() -> {
               log.info("Image request {} not found", civilStatusApprovalRequestDTO);
               return ResponseEntity.ok(responseUtil.error(null, 1011,
                       messageSource.getMessage(ResponseMessageUtil.CIVIL_STATUS_NOT_FOUND, new Object[]{civilStatusApprovalRequestDTO.getId()}, locale)));

           });


       }catch (Exception e) {
           log.error(e);
           throw e;
       }
    }

    private void notifyAdminTeamOnCivilStatusApproval(com.dtech.admin.model.MaritalStatus maritalStatus, String hrUsername) {
        UserPersonalDetails employee = maritalStatus.getApplicationUser() != null
                ? maritalStatus.getApplicationUser().getUserPersonalDetails()
                : null;
        CompanyTypes company = employee != null
                && employee.getUserCompanyDetails() != null
                ? employee.getUserCompanyDetails().getCompanyTypes()
                : null;
        String employeeCompanyCode = company != null ? company.getCode() : null;

        List<WebUser> recipients = webUserRepository.findAllByStatus(Status.ACTIVE).stream()
                .filter(user -> user.getUserRole() != null
                        && org.springframework.util.StringUtils.hasText(user.getUserRole().getCode()))
                .filter(user -> CIVIL_STATUS_APPROVAL_ADMIN_ROLE_CODES.stream()
                        .anyMatch(roleCode -> roleCode.equalsIgnoreCase(user.getUserRole().getCode())))
                .filter(user -> !org.springframework.util.StringUtils.hasText(employeeCompanyCode)
                        || user.getCompanies() == null
                        || user.getCompanies().isEmpty()
                        || user.getCompanies().stream()
                        .anyMatch(assignedCompany -> Status.ACTIVE.equals(assignedCompany.getStatus())
                                && employeeCompanyCode.equalsIgnoreCase(assignedCompany.getCode())))
                .toList();

        emailNotificationService.notifyCivilStatusApprovedByHr(recipients, maritalStatus, hrUsername);
    }

    private void notifyEmployeeOnCivilStatusRejection(com.dtech.admin.model.MaritalStatus maritalStatus) {
        try {
            String mobile = Optional.ofNullable(maritalStatus)
                    .map(com.dtech.admin.model.MaritalStatus::getApplicationUser)
                    .map(applicationUser -> {
                        if (StringUtils.hasText(applicationUser.getPrimaryMobile())) {
                            return applicationUser.getPrimaryMobile();
                        }
                        UserPersonalDetails personalDetails = applicationUser.getUserPersonalDetails();
                        return personalDetails != null ? personalDetails.getMobileNo() : null;
                    })
                    .orElse(null);

            if (!StringUtils.hasText(mobile)) {
                log.warn("Skipping civil status rejection SMS. Employee mobile not found for marital status {}", maritalStatus != null ? maritalStatus.getId() : null);
                return;
            }

            messageService.sendMessageAsync(MessageType.CIVIL_STATUS_REJECTED, "", "", mobile);
        } catch (Exception ex) {
            log.error("Failed to send civil status rejection SMS for marital status {}", maritalStatus != null ? maritalStatus.getId() : null, ex);
        }
    }


}

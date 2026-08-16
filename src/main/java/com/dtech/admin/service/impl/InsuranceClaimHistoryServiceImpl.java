package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.ClaimRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.ClaimsRequestResponseDTO;
import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.enums.*;
import com.dtech.admin.mapper.audit.CustomerApprovalAuditMapper;
import com.dtech.admin.mapper.entityToDto.ClaimsApprovalEntityToDto;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.CompanyAccessService;
import com.dtech.admin.service.InsuranceClaimHistoryService;
import com.dtech.admin.specifications.ClaimsApprovalSpecification;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class InsuranceClaimHistoryServiceImpl implements InsuranceClaimHistoryService {

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
    private final RemarkRepository remarkRepository;

    @Autowired
    private final CompanyAccessService companyAccessService;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Override
    @Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Insurance claims  history request {} ", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.INCH.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Workflow.values())
                    .filter(wf -> Workflow.APPROVED.name().equals(wf.name()) || Workflow.REJECTED.name().equals(wf.name()))
                    .map(wf -> new SimpleBaseDTO(wf.name(), wf.getDescription()))
                    .toList();

            List<SimpleBaseDTO> treatmentCategory = treatmentCategoryRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> remarks = remarkRepository.findAllByRemarkCategoryAndStatus(RemarkCategory.INSURANCE, Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> treatment = treatmentRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getTreatmentCode(), val.getTreatmentDescription())).toList();

            List<SimpleBaseDTO> relationCategory = Arrays.stream(RelationCategory.values())
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> companyTypes = companyAccessService.activeCompanies(channelRequestDTO.getUsername()).stream().map(
                    val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> staffCategories = staffCategoriesRepository.findAllByStatus(Status.ACTIVE).stream().map(
                    val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            responseMap.put("treatmentCategory", treatmentCategory);
            responseMap.put("treatment", treatment);
            responseMap.put("relationCategory", relationCategory);
            responseMap.put("remarks", remarks);
            responseMap.put("company", companyTypes);
            responseMap.put("staffCategories", staffCategories);

            auditLogService.log(WebPage.INCH.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.INCH.name()}, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


    @Override
    @Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ClaimRequestSearchDTO> paginationRequest, Locale locale) {
        try {
            log.info("Insurance claims  filter list {} ", paginationRequest);

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);

            Specification<InsuranceClaimsRequest> specification = Objects.nonNull(paginationRequest.getSearch())
                    ? ClaimsApprovalSpecification.getSpecification(paginationRequest.getSearch(), true)
                    : ClaimsApprovalSpecification.getSpecification(true);
            specification = specification.and(CompanyScopeSpecification.companyCodeIn(
                    companyAccessService.activeCompanyCodes(paginationRequest.getUsername()),
                    "employee", "userPersonalDetails", "userCompanyDetails", "companyTypes", "code"));
            Page<InsuranceClaimsRequest> insuranceClaimsRequests = insuranceClaimsRequestRepository.findAll(specification, pageable);
            log.info("Insurance claims  filter records {}", insuranceClaimsRequests);
            long totalElements = insuranceClaimsRequestRepository.count(specification);
            log.info("Insurance claims filter records map start");

            List<ClaimsRequestResponseDTO> responseDTOList = insuranceClaimsRequests.stream()
                    .map(claim -> applyClaimStaffCategoryToEmployee(claimsApprovalEntityToDto.mapClaimsApproval(claim, false))).toList();
            log.info("Insurance claims  filter records map finish");
//            List<String> newAuditList = customerApprovalAuditMapper.mapToDTOAudit(insuranceClaimsRequests.stream().toList());
//            auditLogService.log(WebPage.INCH.name(), WebTask.SEARCH.name(), AuditTask.SEARCH_FILTER.getDescription(), paginationRequest.getIp(), paginationRequest.getUserAgent(), gson.toJson(newAuditList), null, paginationRequest.getUsername());
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<ClaimsRequestResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIM_HISTORY_DETAILS_FILTER_LIST_SUCCESSFULLY,
                            null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public ResponseEntity<ApiResponse<Object>> view(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Claims history request details view {}", claimRequestDTO);
            return insuranceClaimsRequestRepository.findById(claimRequestDTO.getId())
                    .filter(claimsRequest -> canAccess(claimsRequest, claimRequestDTO.getUsername()))
                    .map(claimsRequest -> {
                ClaimsRequestResponseDTO claimsRequestResponseDTO =
                        applyClaimStaffCategoryToEmployee(claimsApprovalEntityToDto.mapClaimsApproval(claimsRequest, true));
                List<String> newAuditList = customerApprovalAuditMapper.mapToDTOAudit(List.of(claimsRequest));
                auditLogService.log(WebPage.INCH.name(), WebTask.VIEW.name(), AuditTask.VIEW_DATA.getDescription(), claimRequestDTO.getIp(), claimRequestDTO.getUserAgent(), gson.toJson(newAuditList), null, claimRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.success((Object) claimsRequestResponseDTO, messageSource.getMessage(ResponseMessageUtil.INSURANCE_CLAIM_HISTORY_DETAILS_RETRIEVE_SUCCESSFULLY, null, locale)));

            }).orElseGet(() -> {
                log.info("Claim history details not found {}", claimRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1051, messageSource.getMessage(ResponseMessageUtil.CLAIMS_DETAILS_NOT_FOUND, new Object[]{claimRequestDTO.getId()}, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private ClaimsRequestResponseDTO applyClaimStaffCategoryToEmployee(ClaimsRequestResponseDTO dto) {
        if (dto == null
                || dto.getEmployee() == null
                || dto.getEmployee().getUserPersonalDetails() == null
                || dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails() == null
                || dto.getStaffCategoryCode() == null
                || dto.getStaffCategoryCode().isBlank()) {
            return dto;
        }

        dto.getEmployee()
                .getUserPersonalDetails()
                .getUserCompanyDetails()
                .setStaffCategories(new SimpleBaseDTO(dto.getStaffCategoryCode(), dto.getStaffCategoryDescription()));
        return dto;
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
}

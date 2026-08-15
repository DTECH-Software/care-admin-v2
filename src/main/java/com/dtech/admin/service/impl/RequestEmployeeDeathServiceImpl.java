package com.dtech.admin.service.impl;

import com.dtech.admin.dto.*;
import com.dtech.admin.dto.request.*;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.DeathRequestResponseDTO;
import com.dtech.admin.dto.search.ClaimRequestSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.enums.DeathBeneficiary;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.mapper.entityToDto.DeathApprovalEntityToDto;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.RequestEmployeeDeathService;
import com.dtech.admin.service.DocumentStorageService;
import com.dtech.admin.specifications.DeathApprovalSpecification;
import com.dtech.admin.util.*;
import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class RequestEmployeeDeathServiceImpl implements RequestEmployeeDeathService {

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
    private StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private CompanyTypeRepository companyTypeRepository;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private WebUserRepository webUserRepository;

    @Autowired
    private final DocumentStorageService documentStorageService;
    @Autowired
    private ApprovalWorkFlowRepository approvalWorkFlowRepository;

    @Autowired
    private final EntityManager entityManager;
    @Autowired
    private DeathClaimRequestRepository deathClaimRequestRepository;
    @Autowired
    private DeathBeneficiaryRepository deathBeneficiaryRepository;

    @Autowired
    private final DeathApprovalEntityToDto deathApprovalEntityToDto;

    @Override
    public ResponseEntity<ApiResponse<Object>> getReferenceDate(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {

            log.info("Request death request {} ", channelRequestDTO);
            Map<String, Object> responseMap = new HashMap<>();

            AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.
                    getPrivileges(channelRequestDTO.getUsername(), WebPage.RDDF.name());

            List<SimpleBaseDTO> defaultStatus = Arrays.stream(Workflow.values())
                    .filter(status -> !Status.ACTIVE.name().equals(status.name()))
                    .map(st -> new SimpleBaseDTO(st.name(), st.getDescription())).toList();

            List<SimpleBaseDTO> staffCategory = staffCategoriesRepository.findAllByStatus(Status.ACTIVE)
                    .stream().map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription())).toList();

            List<SimpleBaseDTO> companyTypes = getEligibleCompanies(channelRequestDTO.getUsername());
            Set<String> eligibleCompanyCodes = getEligibleCompanyCodes(channelRequestDTO.getUsername());

            List<SimpleBaseDTO> employee = applicationUserRepository.findAll().stream()
                    .filter(val -> eligibleCompanyCodes.contains(val.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode()))
                    .map(val -> new SimpleBaseDTO(String.valueOf(val.getId()), val.getUserPersonalDetails().getFirstName() + " " + val.getUserPersonalDetails().getLastName()))
                    .toList();

            responseMap.put("privileges", privileges);
            responseMap.put("defaultStatus", defaultStatus);
            responseMap.put("employee", employee);
            responseMap.put("staffCategory", staffCategory);
            responseMap.put("company", companyTypes);

            auditLogService.log(WebPage.RDDF.name(), WebTask.REF_DATA.name(), AuditTask.GETTING_ALL_REFERENCE_DATA.getDescription(), channelRequestDTO.getIp(), channelRequestDTO.getUserAgent(), gson.toJson(responseMap), null, channelRequestDTO.getUsername());
            return ResponseEntity.ok().body(responseUtil.success(responseMap, messageSource.getMessage(ResponseMessageUtil.REFERENCE_DATA_RETRIEVED_SUCCESS, new Object[]{WebPage.RDDF.name()}, locale)));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Object>> request(EmployeeDeathRequestDTO employeeDeathRequestDTO, Locale locale) {
        try {
            log.info("Request death request {} ", employeeDeathRequestDTO);
            return applicationUserRepository.findById(employeeDeathRequestDTO.getId()).map(user -> {

                List<Document> uploadSupportingDocument = employeeDeathRequestDTO.getDocuments().stream().map(doc -> {
                    log.info("Upload supporting document from death request employeeId={}", employeeDeathRequestDTO.getId());
                    try {
                        return uploadImage(doc.getType(), doc.getFile(), doc.getFileType(), doc.getFileName());
                    } catch (
                            IOException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

                com.dtech.admin.model.DeathBeneficiary deathBeneficiary = deathBeneficiaryRepository.findByCodeAndStatus(DeathBeneficiary.EMPLOYEE, Status.ACTIVE).get();

                ApprovalWorkFlow approvalWorkFlow = updateApprovalData();
                saveDeathClaimRequest(employeeDeathRequestDTO, user, PaymentType.FULL, approvalWorkFlow, deathBeneficiary, uploadSupportingDocument);
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.DEATH_CLAIM_REQUEST_SUCCESSFULLY, null, locale)));

            }).orElseGet(() -> {
                log.info("Employee not found: {}", employeeDeathRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1050,
                        messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_USER_DETAILS_NOT_FOUND,
                                new Object[]{employeeDeathRequestDTO.getId()}, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void saveDeathClaimRequest(EmployeeDeathRequestDTO employeeDeathRequestDTO,
                                         ApplicationUser applicationUser,
                                         PaymentType paymentType,
                                         ApprovalWorkFlow approvalWorkFlow,
                                         com.dtech.admin.model.DeathBeneficiary deathBeneficiary, List<Document> uploadSupportingDocument) {
        try {
            log.info("Save death claim request death{}", employeeDeathRequestDTO);
            ClaimRequestIdGen claimRequestIdGen = ClaimRequestIdGen
                    .builder().year(String.valueOf(LocalDate.now().getYear()))
                    .company(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode())
                    .staffCategory(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode())
                    .build();
            RequestIdGenUtil requestIdGenUtil = new RequestIdGenUtil(false);
            log.info("Generate request id death {}", claimRequestIdGen);
            String claimRequestId = (String) requestIdGenUtil.generate(entityManager.unwrap(SharedSessionContractImplementor.class), claimRequestIdGen);
            log.info("after generate request id death {}", claimRequestId);
            DeathClaimRequest deathClaimRequest = new DeathClaimRequest();
            deathClaimRequest.setDeathDate(employeeDeathRequestDTO.getDeathDate());
            deathClaimRequest.setRequestId(claimRequestId);
            deathClaimRequest.setRequestStatus(Workflow.UNDER_REVIEW);
            deathClaimRequest.setRemark(employeeDeathRequestDTO.getRemark());
            deathClaimRequest.setPaymentType(paymentType);
            deathClaimRequest.setUtilizeAmount(deathBeneficiary.getClaimLimit());
            deathClaimRequest.setClaimsDependents(null);
            deathClaimRequest.setEmployee(applicationUser);
            deathClaimRequest.setDeathBeneficiary(deathBeneficiary);
            deathClaimRequest.setDocuments(uploadSupportingDocument);
            deathClaimRequest.setApprovalWorkFlows(List.of(approvalWorkFlow));
            deathClaimRequest.setApprovalLevel(ApprovalLevel.LEVEL01);
            log.info("Save death claim request {}", employeeDeathRequestDTO);
            deathClaimRequestRepository.saveAndFlush(deathClaimRequest);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected ApprovalWorkFlow updateApprovalData() {
        log.info("Update approval process data");
        ApprovalWorkFlow approvalWorkFlow = new ApprovalWorkFlow();
        approvalWorkFlow.setApprovalLevel(ApprovalLevel.LEVEL01);
        approvalWorkFlow.setStatus(Workflow.UNDER_REVIEW);
        return approvalWorkFlowRepository.saveAndFlush(approvalWorkFlow);
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
            String baseConvert = Base64.getEncoder().encodeToString(dto.getDocument());
            document = documentStorageService.saveAdminDocument(document, baseConvert);
            log.info("image upload success id={} type={} fileName={} fileType={}",
                    document.getId(), document.getType(), document.getFileName(), document.getFileType());
            return document;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> filterList(PaginationRequest<ClaimRequestSearchDTO> paginationRequest, Locale locale) {
        try {

            Pageable pageable = PaginationUtil.getPageable(paginationRequest);
            Set<String> eligibleCompanyCodes = getEligibleCompanyCodes(paginationRequest.getUsername());

            Page<DeathClaimRequest> claimsRequests = Objects.nonNull(paginationRequest.getSearch()) ?
                    deathClaimRequestRepository.findAll(DeathApprovalSpecification.getSpecification(paginationRequest.getSearch(), false, true, true,false, eligibleCompanyCodes), pageable) :
                    deathClaimRequestRepository.findAll(DeathApprovalSpecification.getSpecification(false, true,true,false, eligibleCompanyCodes), pageable);
            log.info("Filter records {}", claimsRequests);
            long totalElements = Objects.nonNull(paginationRequest.getSearch()) ?
                    deathClaimRequestRepository.count(DeathApprovalSpecification.getSpecification(paginationRequest.getSearch(), false, true,true,false, eligibleCompanyCodes)) :
                    deathClaimRequestRepository.count(DeathApprovalSpecification.getSpecification(false, true,true,false, eligibleCompanyCodes));
            log.info("Total elements count records death{}", totalElements);
            log.info("Filter list data fetching death success");
            List<DeathRequestResponseDTO> responseDTOList = claimsRequests.stream()
                    .map(claim -> deathApprovalEntityToDto.mapClaimsApproval(claim, false)).toList();
            log.info("Filter list death {} success", responseDTOList);
            return ResponseEntity.ok().body(responseUtil.success((Object) new PagingResult<DeathRequestResponseDTO>(responseDTOList, responseDTOList.size(), totalElements),
                    messageSource.getMessage(ResponseMessageUtil.DEATH_DETAILS_FILTER_LIST_SUCCESSFULLY,
                            null, locale)));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Object>> view(ClaimRequestDTO claimRequestDTO, Locale locale) {
        return deathClaimRequestRepository.findById(claimRequestDTO.getId()).map(claimsRequest -> {
            DeathRequestResponseDTO deathRequestResponseDTO = deathApprovalEntityToDto.mapClaimsApproval(claimsRequest, true);

            com.dtech.admin.model.DeathBeneficiary deathBeneficiary = deathBeneficiaryRepository.
                    findByCodeAndStatus(DeathBeneficiary.EMPLOYEE, Status.ACTIVE).orElse(null);

            deathRequestResponseDTO.setDeathLimit(deathBeneficiary != null ? deathBeneficiary.getClaimLimit() : null);

            return ResponseEntity.ok().body(responseUtil.success((Object) deathRequestResponseDTO, messageSource.getMessage(ResponseMessageUtil.DEATH_DETAILS_RETRIEVE_SUCCESSFULLY, null, locale)));

        }).orElseGet(() -> {
            log.info("Claim death details not found {}", claimRequestDTO.getId());
            return ResponseEntity.ok().body(responseUtil.error(null, 1043, messageSource.getMessage(ResponseMessageUtil.DEATH_DETAILS_NOT_FOUND, new Object[]{claimRequestDTO.getId()}, locale)));
        });
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
}

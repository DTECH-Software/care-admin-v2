/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 11:42 AM
 * <p>
 */

package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.response.ApprovalWorkFlowResponseDTO;
import com.dtech.admin.dto.response.ApprovalRejectReasonResponseDTO;
import com.dtech.admin.dto.response.ClaimsRequestResponseDTO;
import com.dtech.admin.dto.response.DocumentDownloadResponseDTO;
import com.dtech.admin.dto.response.EmployeeRejoinDetailsResponseDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.UserPersonalDetailsRepository;
import com.dtech.admin.service.DocumentStorageService;
import com.dtech.admin.util.ApprovalRemarkUtil;
import com.dtech.admin.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class ClaimsApprovalEntityToDto {

    private final ModelMapper modelMapper;
    private final UserPersonalDetailsRepository userPersonalDetailsRepository;
    private final DocumentStorageService documentStorageService;

    public ClaimsRequestResponseDTO mapClaimsApproval(InsuranceClaimsRequest insuranceClaimsRequest, boolean isDocument) {
        try {
            log.info("mapClaimsApproval mapper {} ", insuranceClaimsRequest.getId());

            modelMapper.typeMap(InsuranceClaimsRequest.class, ClaimsRequestResponseDTO.class)
                    .addMappings(mp ->
                            mp.skip(ClaimsRequestResponseDTO::setApprovalWorkFlow));

            ClaimsRequestResponseDTO dto = modelMapper.map(insuranceClaimsRequest, ClaimsRequestResponseDTO.class);
            dto.setRequestStatusDescription(Workflow.valueOf(dto.getRequestStatus()).getDescription());
            dto.setApprovalLevelDescription(ApprovalLevel.valueOf(dto.getApprovalLevel()).getDescription());
            dto.getEmployee().getUserPersonalDetails().setAge(DateTimeUtil.getAge(String.valueOf(insuranceClaimsRequest.getEmployee().getUserPersonalDetails().getDob())));
            dto.getEmployee().getUserPersonalDetails().setGenderDescription(insuranceClaimsRequest.getEmployee().getUserPersonalDetails().getGender().getDescription());
            if (dto.getEmployee() != null
                    && dto.getEmployee().getUserPersonalDetails() != null
                    && dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails() != null
                    && dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getFacility() != null) {
                dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails().setFacilityDescription(
                        Facility.valueOf(dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails().getFacility()).getDescription()
                );
            }
            dto.setCreatedDate(insuranceClaimsRequest.getCreatedDate());
            populateClaimStaffCategory(dto, insuranceClaimsRequest);
            populateEmployeeDatesAndRejoinDetails(dto, insuranceClaimsRequest.getEmployee().getUserPersonalDetails());
            normalizeCompanyPermanentDatesForUi(dto);
            if (insuranceClaimsRequest.getClaimsDependents() != null) {
                log.info("get Age fro insurance depende");
                dto.getClaimsDependents().setAge(DateTimeUtil.getAge(String.valueOf(insuranceClaimsRequest.getClaimsDependents().getDob())));
            }

            List<ApprovalWorkFlowResponseDTO> list = insuranceClaimsRequest.getApprovalWorkFlows().stream().map(ap -> {
                ApprovalWorkFlowResponseDTO approvalWorkFlowResponseDTO = new ApprovalWorkFlowResponseDTO();

                approvalWorkFlowResponseDTO.setId(ap.getId());
                approvalWorkFlowResponseDTO.setApprovedUser(ap.getApprovedUser());
                approvalWorkFlowResponseDTO.setApprovedDate(ap.getApprovedDate());
                approvalWorkFlowResponseDTO.setApprovalLevel(ap.getApprovalLevel().name());
                approvalWorkFlowResponseDTO.setApprovalLevelDescription(ap.getApprovalLevel().getDescription());
                approvalWorkFlowResponseDTO.setStatus(ap.getStatus().name());
                approvalWorkFlowResponseDTO.setStatusDescription(Workflow.valueOf(ap.getStatus().name()).getDescription());
                approvalWorkFlowResponseDTO.setRejectReasons(mapRejectReasons(ap));
                approvalWorkFlowResponseDTO.setRejectedAmount(sumRejectAmounts(approvalWorkFlowResponseDTO.getRejectReasons()));
                String rejectRemark = ApprovalRemarkUtil.resolveWorkflowRemark(ap);
                approvalWorkFlowResponseDTO.setRejectedRemark(rejectRemark != null ? rejectRemark : ap.getRejectedRemark());
                approvalWorkFlowResponseDTO.setApprovedAmount(ap.getApprovedAmount());
                approvalWorkFlowResponseDTO.setPolicyId(ap.getPolicy() != null ? ap.getPolicy().getId() : null);
                approvalWorkFlowResponseDTO.setPolicyDescription(ap.getPolicy() != null
                        ? ap.getPolicy().getFromDate().toString() + " to " + ap.getPolicy().getToDate().toString() + " " + ap.getPolicy().getStaffCategories().getDescription()
                        : null);

                return approvalWorkFlowResponseDTO;
            }).toList();

            dto.setApprovalWorkFlow(list);


            if (dto.getClaimsDependents() != null) {
                dto.getClaimsDependents().setRelationCategoryDescription(RelationCategory
                        .valueOf(dto.getClaimsDependents().getRelationCategory()).getDescription());

                dto.getClaimsDependents().setDependentCategoryDescription(DependentCategory.valueOf(dto.getClaimsDependents()
                        .getDependentCategory()).getDescription());

                dto.getClaimsDependents().setGenderDescription(Gender.valueOf(dto.getClaimsDependents().getGender()).getDescription());

                dto.getClaimsDependents().setEligibleFacilityDescription(Facility.valueOf(dto.getClaimsDependents().getEligibleFacility())
                        .getDescription());

            }

            if (isDocument) {
                List<DocumentDownloadResponseDTO> collect = insuranceClaimsRequest.getInsuranceClaimsDetails().getDocuments().stream().map((document -> {
                    return new DocumentDownloadResponseDTO(String.valueOf(document.getType()), document.getFileName(), document.getFileType(), documentStorageService.getBase64(document));
                })).collect(Collectors.toList());
                dto.getInsuranceClaimsDetails().setDocuments(collect);
            } else if (dto.getInsuranceClaimsDetails() != null) {
                // Skip document payload for list endpoints to avoid unnecessary loading
                dto.getInsuranceClaimsDetails().setDocuments(null);
            }

            return dto;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private void populateClaimStaffCategory(ClaimsRequestResponseDTO dto, InsuranceClaimsRequest claim) {
        StaffCategories staffCategory = resolveClaimStaffCategory(claim);
        if (staffCategory == null) {
            return;
        }
        dto.setStaffCategoryCode(staffCategory.getCode());
        dto.setStaffCategoryDescription(staffCategory.getDescription());
    }

    private List<ApprovalRejectReasonResponseDTO> mapRejectReasons(ApprovalWorkFlow workflow) {
        if (workflow.getRejectReasons() == null || workflow.getRejectReasons().isEmpty()) {
            return List.of();
        }
        return workflow.getRejectReasons().stream().map(reason -> {
            ApprovalRejectReasonResponseDTO dto = new ApprovalRejectReasonResponseDTO();
            dto.setId(reason.getId());
            dto.setReasonCode(reason.getReasonCode());
            dto.setReasonDescription(reason.getReasonDescription());
            dto.setReasonCategory(reason.getReasonCategory());
            dto.setAmount(reason.getAmount());
            dto.setRemark(reason.getRemark());
            return dto;
        }).toList();
    }

    private BigDecimal sumRejectAmounts(List<ApprovalRejectReasonResponseDTO> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return reasons.stream()
                .map(ApprovalRejectReasonResponseDTO::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private StaffCategories resolveClaimStaffCategory(InsuranceClaimsRequest claim) {
        InsuranceStaffCategoryPeriod period = null;
        if (claim.getInsuranceDetailsLimit() != null) {
            period = claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod();
        }
        if (period == null && claim.getInsuranceClaimsDetails() != null) {
            period = claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod();
        }
        return period != null ? period.getStaffCategories() : null;
    }

    private void normalizeCompanyPermanentDatesForUi(ClaimsRequestResponseDTO dto) {
        if (dto == null
                || dto.getEmployee() == null
                || dto.getEmployee().getUserPersonalDetails() == null
                || dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails() == null) {
            return;
        }

        var companyDetails = dto.getEmployee().getUserPersonalDetails().getUserCompanyDetails();
        if (companyDetails.getPreviousPermanentDate() == null) {
            companyDetails.setPreviousPermanentDate(companyDetails.getPermanentDate());
            companyDetails.setPermanentDate(null);
        }
    }

    private void populateEmployeeDatesAndRejoinDetails(ClaimsRequestResponseDTO dto, UserPersonalDetails personalDetails) {
        if (dto == null || personalDetails == null) {
            return;
        }

        UserCompanyDetails companyDetails = personalDetails.getUserCompanyDetails();
        if (companyDetails != null) {
            if (companyDetails.getPreviousPermanentDate() != null) {
                dto.setPermanentDate(companyDetails.getPreviousPermanentDate());
                dto.setPromotionDate(companyDetails.getPermanentDate());
            } else {
                dto.setPermanentDate(companyDetails.getPermanentDate());
                dto.setPromotionDate(null);
            }
            dto.setTerminateDate(companyDetails.getTerminateDate());
        }

        EmployeeRejoinDetailsResponseDTO rejoinDetails = buildRejoinDetails(personalDetails);
        if (rejoinDetails != null) {
            dto.setPreviousCompanies(rejoinDetails.getPreviousCompanies());
            dto.setPreviousEpfs(rejoinDetails.getPreviousEpfs());
            if (dto.getEmployee() != null) {
                dto.getEmployee().setRejoinDetails(rejoinDetails);
            }
        }
    }

    private EmployeeRejoinDetailsResponseDTO buildRejoinDetails(UserPersonalDetails currentUser) {
        if (currentUser == null || !StringUtils.hasText(currentUser.getNic()) || currentUser.getId() == null) {
            return null;
        }

        List<UserPersonalDetails> previousProfiles = userPersonalDetailsRepository
                .findAllByNicIgnoreCaseAndUserStatusAndIdNotOrderByIdDesc(currentUser.getNic(), Status.INACTIVE, currentUser.getId());

        if (previousProfiles.isEmpty()) {
            return null;
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
            return null;
        }

        EmployeeRejoinDetailsResponseDTO rejoinDetails = new EmployeeRejoinDetailsResponseDTO();
        rejoinDetails.setPreviousCompanies(new ArrayList<>(previousCompanies.values()));
        rejoinDetails.setPreviousEpfs(new ArrayList<>(previousEpfs));
        return rejoinDetails;
    }

    private String normalizeValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

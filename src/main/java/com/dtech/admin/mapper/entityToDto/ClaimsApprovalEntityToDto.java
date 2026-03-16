/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 11:42 AM
 * <p>
 */

package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.response.ApprovalWorkFlowResponseDTO;
import com.dtech.admin.dto.response.ClaimsRequestResponseDTO;
import com.dtech.admin.dto.response.DocumentDownloadResponseDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class ClaimsApprovalEntityToDto {

    private final ModelMapper modelMapper;

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
            dto.setCreatedDate(insuranceClaimsRequest.getCreatedDate());
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
                approvalWorkFlowResponseDTO.setRejectedRemark(ap.getRejectedRemark());
                approvalWorkFlowResponseDTO.setApprovedAmount(ap.getApprovedAmount());
                approvalWorkFlowResponseDTO.setPolicyId(ap.getPolicy() != null ? ap.getPolicy().getId() : null);

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
                    return new DocumentDownloadResponseDTO(String.valueOf(document.getType()), document.getFileName(), document.getFileType(), document.getDoc());
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
}

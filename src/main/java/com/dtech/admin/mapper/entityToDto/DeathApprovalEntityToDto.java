/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 11:42 AM
 * <p>
 */

package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.response.ApprovalWorkFlowResponseDTO;
import com.dtech.admin.dto.response.DeathRequestResponseDTO;
import com.dtech.admin.dto.response.DocumentDownloadResponseDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.DeathClaimRequest;
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
public class DeathApprovalEntityToDto {

    private final ModelMapper modelMapper;

    public DeathRequestResponseDTO mapClaimsApproval(DeathClaimRequest deathClaimRequest, boolean isDocument) {
        try {
            log.info("mapClaimsApproval death mapper {} ", deathClaimRequest.getId());

            modelMapper.typeMap(DeathClaimRequest.class, DeathRequestResponseDTO.class)
                    .addMappings(mp ->{
                        mp.skip(DeathRequestResponseDTO::setApprovalWorkFlow);
                            });


            DeathRequestResponseDTO dto = modelMapper.map(deathClaimRequest, DeathRequestResponseDTO.class);
            dto.setRequestStatusDescription(Workflow.valueOf(dto.getRequestStatus()).getDescription());
            dto.setCreatedDate(deathClaimRequest.getCreatedDate());
            dto.setApprovalLevelDescription(ApprovalLevel.valueOf(dto.getApprovalLevel()).getDescription());
            dto.getEmployee().setAge(DateTimeUtil.getAge(String.valueOf(deathClaimRequest.getEmployee().getUserPersonalDetails().getDob())));
            dto.getEmployee().setGenderDescription(deathClaimRequest.getEmployee().getUserPersonalDetails().getGender().getDescription());
            dto.setCreatedDate(deathClaimRequest.getCreatedDate());
            if(deathClaimRequest.getClaimsDependents() != null) {
                dto.getClaimsDependents().setAge(DateTimeUtil.getAge(String.valueOf(deathClaimRequest.getClaimsDependents().getDob())));
            }
            List<ApprovalWorkFlowResponseDTO> list = deathClaimRequest.getApprovalWorkFlows().stream().map(ap -> {
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
                List<DocumentDownloadResponseDTO> collect = deathClaimRequest.getDocuments().stream().map((document -> {
                    return new DocumentDownloadResponseDTO(String.valueOf(document.getType()), document.getFileName(), document.getFileType(), document.getDoc());
                })).collect(Collectors.toList());
                dto.setDocuments(collect);
            }


            return dto;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}

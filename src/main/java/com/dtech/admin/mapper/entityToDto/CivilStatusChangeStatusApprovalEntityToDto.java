package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.response.DocumentDownloadResponseDTO;
import com.dtech.admin.dto.response.MaritalStatusApprovalResponseDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.MaritalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Log4j2
@Component
@RequiredArgsConstructor
public class CivilStatusChangeStatusApprovalEntityToDto {

    private final ModelMapper modelMapper;

    public MaritalStatusApprovalResponseDTO mapCivilStatusApproval(MaritalStatus maritalStatus) {
        try {
            log.info("mapCivilStatusApproval death mapper {} ", maritalStatus.getId());

            MaritalStatusApprovalResponseDTO dto = modelMapper.map(maritalStatus, MaritalStatusApprovalResponseDTO.class);
            dto.setStatusDescription(Workflow.valueOf(dto.getStatus()).getDescription());
            dto.setMaritalStatusDescription(com.dtech.admin.enums.MaritalStatus.valueOf(maritalStatus.getMaritalStatus().name()).getDescription());

            if (maritalStatus.getApplicationUser() != null) {
                dto.setEmployeeName(maritalStatus.getApplicationUser().getUserPersonalDetails().getTitle().getDescription() + " " +
                        maritalStatus.getApplicationUser().getUserPersonalDetails().getFirstName() +" "+
                        maritalStatus.getApplicationUser().getUserPersonalDetails().getLastName());

                dto.setNic(maritalStatus.getApplicationUser().getUserPersonalDetails().getNic());
                dto.setEpfNo(maritalStatus.getApplicationUser().getUserPersonalDetails().getEpfNo());
                dto.setCompany(maritalStatus.getApplicationUser().getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getDescription());
                dto.setStaffCategory(maritalStatus.getApplicationUser().getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getDescription());

            }

            return dto;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


    public List<DocumentDownloadResponseDTO> getDocument(MaritalStatus maritalStatus) {
        try {
            log.info("mapCivilStatusApproval death mapper {} ", maritalStatus.getId());

                return maritalStatus.getDocuments().stream().map((document -> {
                    return new DocumentDownloadResponseDTO(String.valueOf(document.getType()), document.getFileName(), document.getFileType(), document.getDoc());
                })).collect(Collectors.toList());

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}

package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.response.ApplicationUserResponseDTO;
import com.dtech.admin.dto.response.DependentDetailsResponseDTO;
import com.dtech.admin.dto.response.DocumentDownloadResponseDTO;
import com.dtech.admin.dto.response.UserAddressResponseDTO;
import com.dtech.admin.dto.response.UserCompanyDetailsResponseDTO;
import com.dtech.admin.dto.response.UserPersonalDetailsResponseDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.Document;
import com.dtech.admin.model.InsurancePolicy;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.StaffTypes;
import com.dtech.admin.model.UserAddress;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.util.DateTimeUtil;
import com.dtech.admin.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class DependentDetailsMapperEntityToDto {

    private final ModelMapper modelMapper;
    private final DocumentStorageService documentStorageService;

    public DependentDetailsResponseDTO mapDependentDetails(ClaimsDependents claimsDependents) {
        return mapDependentDetails(claimsDependents, true);
    }

    /**
     * Maps the normal dependent response but leaves document Base64 content out.
     * Document metadata is retained so the response shape remains compatible.
     */
    public DependentDetailsResponseDTO mapDependentDetailsWithoutDocumentContent(ClaimsDependents claimsDependents) {
        return mapDependentDetails(claimsDependents, false);
    }

    private DependentDetailsResponseDTO mapDependentDetails(ClaimsDependents claimsDependents,
                                                            boolean includeDocumentContent) {
        try {
            log.info("mapDependentDetails mapper {} ", claimsDependents.getId());
            DependentDetailsResponseDTO dependentDetailsResponseDTO = modelMapper.map(claimsDependents, DependentDetailsResponseDTO.class);
            log.info("mapDependentDetails mapper model {} ", dependentDetailsResponseDTO.toString());
            fillDependentFields(dependentDetailsResponseDTO, claimsDependents, includeDocumentContent);
            return dependentDetailsResponseDTO;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private void fillDependentFields(DependentDetailsResponseDTO dto, ClaimsDependents dependent,
                                     boolean includeDocumentContent) {
        // Keep the dependent audit dates explicit in the API response. These are
        // displayed in the UI as Create Date and Modification Date.
        dto.setCreatedDate(dependent.getCreatedDate());
        dto.setLastModifiedDate(dependent.getLastModifiedDate());
        dto.setDependentCategory(enumName(dependent.getDependentCategory()));
        dto.setDependentCategoryDescription(description(dependent.getDependentCategory()));
        dto.setGender(enumName(dependent.getGender()));
        dto.setGenderDescription(description(dependent.getGender()));
        dto.setEligibleFacility(enumName(dependent.getEligibleFacility()));
        dto.setEligibleFacilityDescription(description(dependent.getEligibleFacility()));
        dto.setRelationCategory(enumName(dependent.getRelationCategory()));
        dto.setRelationCategoryDescription(description(dependent.getRelationCategory()));
        dto.setStatus(enumName(dependent.getStatus()));
        dto.setStatusDescription(description(dependent.getStatus()));
        dto.setAge(dependent.getDob() != null ? DateTimeUtil.getAge(String.valueOf(dependent.getDob())) : 0);
        dto.setRemark(dependent.getRemark());
        dto.setApprovedDate(dependent.getApprovedDate());
        dto.setApprovedUser(dependent.getApprovedUser());
        dto.setLiveStatus(dependent.getLiveStatus());
        dto.setApplicationUser(mapApplicationUser(dependent.getApplicationUser(), includeDocumentContent));
        dto.setAttachment(mapDocuments(dependent.getDocuments(), includeDocumentContent));
    }

    private ApplicationUserResponseDTO mapApplicationUser(ApplicationUser user, boolean includeDocumentContent) {
        if (user == null) {
            return null;
        }
        ApplicationUserResponseDTO dto = modelMapper.map(user, ApplicationUserResponseDTO.class);
        dto.setLoginStatus(enumName(user.getLoginStatus()));
        dto.setLoginStatusDescription(description(user.getLoginStatus()));
        dto.setUserPersonalDetails(mapPersonalDetails(user.getUserPersonalDetails(), includeDocumentContent));
        if (user.getUserPersonalDetails() != null) {
            dto.setGender(enumName(user.getUserPersonalDetails().getGender()));
            dto.setGenderDescription(description(user.getUserPersonalDetails().getGender()));
            dto.setAge(user.getUserPersonalDetails().getDob() != null
                    ? DateTimeUtil.getAge(String.valueOf(user.getUserPersonalDetails().getDob()))
                    : 0);
        }
        return dto;
    }

    private UserPersonalDetailsResponseDTO mapPersonalDetails(UserPersonalDetails personalDetails,
                                                              boolean includeDocumentContent) {
        if (personalDetails == null) {
            return null;
        }
        UserPersonalDetailsResponseDTO dto = modelMapper.map(personalDetails, UserPersonalDetailsResponseDTO.class);
        dto.setTitle(enumName(personalDetails.getTitle()));
        dto.setTitleDescription(description(personalDetails.getTitle()));
        dto.setGender(enumName(personalDetails.getGender()));
        dto.setGenderDescription(description(personalDetails.getGender()));
        dto.setMaritalStatus(enumName(personalDetails.getMaritalStatus()));
        dto.setMaritalStatusDescription(description(personalDetails.getMaritalStatus()));
        dto.setUserStatus(enumName(personalDetails.getUserStatus()));
        dto.setUserStatusDescription(description(personalDetails.getUserStatus()));
        dto.setAge(personalDetails.getDob() != null ? DateTimeUtil.getAge(String.valueOf(personalDetails.getDob())) : 0);
        dto.setUserAddress(mapAddress(personalDetails.getUserAddress()));
        dto.setUserCompanyDetails(mapCompanyDetails(personalDetails.getUserCompanyDetails(), includeDocumentContent));
        dto.setBirthImg(mapDocument(personalDetails.getBirthImg(), includeDocumentContent));
        return dto;
    }

    private UserAddressResponseDTO mapAddress(UserAddress address) {
        if (address == null) {
            return null;
        }
        UserAddressResponseDTO dto = new UserAddressResponseDTO();
        dto.setStreetNo(address.getStreetNo());
        dto.setStreet1(address.getStreet1());
        dto.setStreet2(address.getStreet2());
        dto.setCity(address.getCity());
        return dto;
    }

    private UserCompanyDetailsResponseDTO mapCompanyDetails(UserCompanyDetails companyDetails,
                                                            boolean includeDocumentContent) {
        if (companyDetails == null) {
            return null;
        }
        UserCompanyDetailsResponseDTO dto = new UserCompanyDetailsResponseDTO();
        dto.setCompanyTypes(simple(companyDetails.getCompanyTypes()));
        dto.setPaymentCompany(simple(companyDetails.getPaymentCompany()));
        dto.setDeathPaymentCompany(simple(companyDetails.getDeathPaymentCompany()));
        dto.setStaffCategories(simple(companyDetails.getStaffCategories()));
        dto.setStaffTypes(simple(companyDetails.getStaffTypes()));
        dto.setDesignation(companyDetails.getDesignation());
        dto.setPermanentDate(companyDetails.getPermanentDate());
        if (companyDetails.getPreviousPermanentDate() == null) {
            dto.setPreviousPermanentDate(companyDetails.getPermanentDate());
        } else {
            dto.setPreviousPermanentDate(companyDetails.getPreviousPermanentDate());
        }
        dto.setTerminateDate(companyDetails.getTerminateDate());
        dto.setInsurancePolicy(simple(companyDetails.getInsurancePolicy()));
        dto.setFacility(enumName(companyDetails.getFacility()));
        dto.setFacilityDescription(description(companyDetails.getFacility()));
        dto.setPromoDoc(mapDocument(companyDetails.getPromoDocs(), includeDocumentContent));
        return dto;
    }

    private List<DocumentDownloadResponseDTO> mapDocuments(List<Document> documents, boolean includeDocumentContent) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        return documents.stream().map(document -> mapDocument(document, includeDocumentContent)).toList();
    }

    private DocumentDownloadResponseDTO mapDocument(Document document, boolean includeDocumentContent) {
        if (document == null) {
            return null;
        }
        DocumentDownloadResponseDTO dto = new DocumentDownloadResponseDTO();
        dto.setType(enumName(document.getType()));
        dto.setFileName(document.getFileName());
        dto.setFileType(document.getFileType());
        if (includeDocumentContent) {
            dto.setDoc(documentStorageService.getBase64(document));
        }
        return dto;
    }

    private SimpleBaseDTO simple(CompanyTypes value) {
        return value == null ? null : new SimpleBaseDTO(value.getCode(), value.getDescription());
    }

    private SimpleBaseDTO simple(StaffCategories value) {
        return value == null ? null : new SimpleBaseDTO(value.getCode(), value.getDescription());
    }

    private SimpleBaseDTO simple(StaffTypes value) {
        return value == null ? null : new SimpleBaseDTO(value.getCode(), value.getDescription());
    }

    private SimpleBaseDTO simple(InsurancePolicy value) {
        return value == null ? null : new SimpleBaseDTO(value.getCode(), value.getDescription());
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String description(Workflow value) {
        return value == null ? null : value.getDescription();
    }

    private String description(DependentCategory value) {
        return value == null ? null : value.getDescription();
    }

    private String description(Gender value) {
        return value == null ? null : value.getDescription();
    }

    private String description(Facility value) {
        return value == null ? null : value.getDescription();
    }

    private String description(RelationCategory value) {
        return value == null ? null : value.getDescription();
    }

    private String description(Status value) {
        return value == null ? null : value.getDescription();
    }

    private String description(Title value) {
        return value == null ? null : value.getDescription();
    }

    private String description(MaritalStatus value) {
        return value == null ? null : value.getDescription();
    }

}

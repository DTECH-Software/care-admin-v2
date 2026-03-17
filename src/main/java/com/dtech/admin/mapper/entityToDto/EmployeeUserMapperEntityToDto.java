/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 11:42 AM
 * <p>
 */

package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.response.ApplicationUserResponseDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class EmployeeUserMapperEntityToDto {

    private  final ModelMapper modelMapper;

    public ApplicationUserResponseDTO mapEmployeeUserDetails(ApplicationUser applicationUser){
        try {
            log.info("mapEmployeeUserDetails mapper {} ", applicationUser.getId());
            ApplicationUserResponseDTO applicationUserResponseDTO = modelMapper.map(applicationUser, ApplicationUserResponseDTO.class);
            log.info("mapEmployeeUserDetails mapper model {} ", applicationUserResponseDTO.toString());

            if (applicationUserResponseDTO.getUserPersonalDetails() != null
                    && applicationUserResponseDTO.getUserPersonalDetails().getUserCompanyDetails() != null
                    && applicationUserResponseDTO.getUserPersonalDetails().getUserCompanyDetails().getPreviousPermanentDate() == null) {
                applicationUserResponseDTO.getUserPersonalDetails().getUserCompanyDetails()
                        .setPreviousPermanentDate(applicationUserResponseDTO.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate());
                applicationUserResponseDTO.getUserPersonalDetails().getUserCompanyDetails().setPermanentDate(null);
            }

            applicationUserResponseDTO.setLoginStatusDescription(Status.valueOf(applicationUser.getLoginStatus().name()).getDescription());

            applicationUserResponseDTO.getUserPersonalDetails().setUserStatusDescription(Status.valueOf(applicationUser.getUserPersonalDetails().getUserStatus().name()).getDescription());
            applicationUserResponseDTO.getUserPersonalDetails().setTitleDescription(Title.valueOf(applicationUser.getUserPersonalDetails().getTitle().name()).getDescription());
            applicationUserResponseDTO.getUserPersonalDetails().setMaritalStatusDescription(MaritalStatus.valueOf(applicationUser.getUserPersonalDetails().getMaritalStatus().name()).getDescription());
            applicationUserResponseDTO.getUserPersonalDetails().setGenderDescription(Gender.valueOf(applicationUser.getUserPersonalDetails().getGender().name()).getDescription());
            applicationUserResponseDTO.getUserPersonalDetails().getUserCompanyDetails().setFacilityDescription(Facility.valueOf(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getFacility().name()).getDescription());
            int age = DateTimeUtil.getAge(String.valueOf(applicationUser.getUserPersonalDetails().getDob()));
            applicationUserResponseDTO.getUserPersonalDetails().setAge(age);
            return applicationUserResponseDTO;
        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }
}

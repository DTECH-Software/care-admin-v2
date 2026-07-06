/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 11:42 AM
 * <p>
 */

package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.response.EmployeeDetailsResponseDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class EmployeeDetailsMapperEntityToDto {
    private static final String DUMMY_MOBILE_PREFIX = "0000";

    private  final ModelMapper modelMapper;

    public EmployeeDetailsResponseDTO mapEmployeeDetails(UserPersonalDetails userPersonalDetails){
        try {
            log.info("mapEmployeeDetails mapper {} ", userPersonalDetails.getId());
            EmployeeDetailsResponseDTO employeeDetailsResponseDTO = modelMapper.map(userPersonalDetails, EmployeeDetailsResponseDTO.class);
            log.info("mapEmployeeDetails mapper model {} ", employeeDetailsResponseDTO.toString());

            if (employeeDetailsResponseDTO.getUserCompanyDetails() != null
                    && employeeDetailsResponseDTO.getUserCompanyDetails().getPreviousPermanentDate() == null) {
                employeeDetailsResponseDTO.getUserCompanyDetails()
                        .setPreviousPermanentDate(employeeDetailsResponseDTO.getUserCompanyDetails().getPermanentDate());
                employeeDetailsResponseDTO.getUserCompanyDetails().setPermanentDate(null);
            }

            employeeDetailsResponseDTO.setUserStatusDescription(Status.valueOf(employeeDetailsResponseDTO.getUserStatus()).getDescription());
            employeeDetailsResponseDTO.setTitleDescription(Title.valueOf(employeeDetailsResponseDTO.getTitle()).getDescription());
            employeeDetailsResponseDTO.setMaritalStatusDescription(MaritalStatus.valueOf(employeeDetailsResponseDTO.getMaritalStatus()).getDescription());
            employeeDetailsResponseDTO.setGenderDescription(Gender.valueOf(employeeDetailsResponseDTO.getGender()).getDescription());
            employeeDetailsResponseDTO.getUserCompanyDetails().setFacilityDescription(Facility.valueOf(employeeDetailsResponseDTO.getUserCompanyDetails().getFacility()).getDescription());
            int age = DateTimeUtil.getAge(String.valueOf(userPersonalDetails.getDob()));
            employeeDetailsResponseDTO.setAge(age);
            employeeDetailsResponseDTO.setNoMobileNumber(isDummyMobile(userPersonalDetails.getMobileNo()));
            return employeeDetailsResponseDTO;
        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }

    private boolean isDummyMobile(String mobileNo) {
        return mobileNo != null && mobileNo.trim().startsWith(DUMMY_MOBILE_PREFIX);
    }
}

package com.dtech.admin.mapper.dtoToEntity;

import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.request.UserCompanyDetailsRequestDTO;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class EmployeeDetailsMapperDtoToEntity {

    private final ModelMapper modelMapper;

    public UserPersonalDetails mapPersonalDetails(EmployeeDetailsRequestDTO employeeDetailsRequestDTO) {
        try {
            log.info("Employee personal mapper start dto to entity");
            UserPersonalDetails userPersonalDetails = modelMapper.map(employeeDetailsRequestDTO, UserPersonalDetails.class);
            log.info("Success Employee personal mapper  dto to entity {} ", userPersonalDetails);
            return userPersonalDetails;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
    public UserCompanyDetails mapCompanyDetails(UserCompanyDetailsRequestDTO userCompanyDetailsRequestDTO) {
        try {
            log.info("Employee company mapper start dto to entity");
            UserCompanyDetails userCompanyDetails = modelMapper.map(userCompanyDetailsRequestDTO, UserCompanyDetails.class);
            log.info("Success Employee company mapper  dto to entity {} ", userCompanyDetails);
            return userCompanyDetails;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}

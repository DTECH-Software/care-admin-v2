package com.dtech.admin.mapper.audit;

import com.dtech.admin.dto.audit.UserPersonalDetailsAuditDTO;
import com.dtech.admin.model.UserPersonalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class EmployeeDetailsAuditMapper {

    private final ModelMapper modelMapper ;

    public List<String> mapToDTOAudit(List<UserPersonalDetails> userPersonalDetails) {
        log.info("Audit mapper by employee details Mapper");

        return userPersonalDetails.stream()
                .map(se -> {
                    log.info("Mapper from employee details {}", se.getId());
                    UserPersonalDetailsAuditDTO dto = modelMapper.map(se, UserPersonalDetailsAuditDTO.class);

                    dto.setUserStatusDescription(se.getUserStatus().getDescription());
                    dto.setTitleDescription(se.getTitle().getDescription());
                    dto.setGenderDescription(se.getGender().getDescription());

                    return dto.toString();
                })
                .collect(Collectors.toList());
    }
}

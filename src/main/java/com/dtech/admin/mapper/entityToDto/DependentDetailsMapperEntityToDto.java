package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.response.DependentDetailsResponseDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class DependentDetailsMapperEntityToDto {

    private final ModelMapper modelMapper;

    public DependentDetailsResponseDTO mapDependentDetails(ClaimsDependents claimsDependents) {
        try {
            log.info("mapDependentDetails mapper {} ", claimsDependents.getId());
            DependentDetailsResponseDTO dependentDetailsResponseDTO = modelMapper.map(claimsDependents, DependentDetailsResponseDTO.class);
            log.info("mapDependentDetails mapper model {} ", dependentDetailsResponseDTO.toString());
            dependentDetailsResponseDTO.setStatusDescription(Workflow.valueOf(dependentDetailsResponseDTO.getStatus()).getDescription());
            dependentDetailsResponseDTO.setDependentCategoryDescription(DependentCategory.valueOf(dependentDetailsResponseDTO.getDependentCategory()).getDescription());
            dependentDetailsResponseDTO.setGenderDescription(Gender.valueOf(dependentDetailsResponseDTO.getGender()).getDescription());
            dependentDetailsResponseDTO.setEligibleFacilityDescription(Facility.valueOf(dependentDetailsResponseDTO.getEligibleFacility()).getDescription());
            dependentDetailsResponseDTO.setRelationCategoryDescription(RelationCategory.valueOf(dependentDetailsResponseDTO.getRelationCategory()).getDescription());
            int age = DateTimeUtil.getAge(String.valueOf(claimsDependents.getDob()));
            dependentDetailsResponseDTO.setAge(age);
            return dependentDetailsResponseDTO;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}

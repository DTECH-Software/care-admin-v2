package com.dtech.admin.mapper.audit;

import com.dtech.admin.dto.audit.DependentDetailsAuditDTO;
import com.dtech.admin.model.ClaimsDependents;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class DependentDetailsAuditMapper {

    private final ModelMapper modelMapper ;

    public List<String> mapToDTOAudit(List<ClaimsDependents> claimsDependents) {
        log.info("Audit mapper by dependent details Mapper");

        return claimsDependents.stream()
                .map(se -> {
                    log.info("Mapper from dependent details {}", se.getId());
                    DependentDetailsAuditDTO dto = modelMapper.map(se, DependentDetailsAuditDTO.class);

                    dto.setStatusDescription(se.getStatus().getDescription());
                    dto.setDependentCategoryDescription(se.getDependentCategory().getDescription());
                    dto.setGenderDescription(se.getGender().getDescription());
                    dto.setEligibleFacilityDescription(se.getEligibleFacility().getDescription());
                    dto.setRelationCategoryDescription(se.getRelationCategory().getDescription());

                    return dto.toString();
                })
                .collect(Collectors.toList());
    }
}

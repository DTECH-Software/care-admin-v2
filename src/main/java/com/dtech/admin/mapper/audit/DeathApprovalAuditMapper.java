package com.dtech.admin.mapper.audit;

import com.dtech.admin.dto.audit.ClaimsRequestAuditDTO;
import com.dtech.admin.model.DeathClaimRequest;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
public class DeathApprovalAuditMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public  List<String> mapToDTOAudit(List<DeathClaimRequest> deathClaimRequests) {
        log.info("Audit mapper by customer death approval Mapper");

        return deathClaimRequests.stream()
                .map(se -> {
                    log.info("Mapper death from customer Mapper {}", se.getId());
                    ClaimsRequestAuditDTO dto = modelMapper.map(se, ClaimsRequestAuditDTO.class);

                    dto.setRequestStatusDescription(se.getRequestStatus().getDescription());
                    return dto.toString();
                })
                .collect(Collectors.toList());
    }
}

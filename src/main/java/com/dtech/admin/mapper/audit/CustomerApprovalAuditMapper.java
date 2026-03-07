package com.dtech.admin.mapper.audit;

import com.dtech.admin.dto.audit.ClaimsRequestAuditDTO;
import com.dtech.admin.dto.audit.CompanyAuditDTO;
import com.dtech.admin.model.InsuranceClaimsRequest;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
public class CustomerApprovalAuditMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public  List<String> mapToDTOAudit(List<InsuranceClaimsRequest> insuranceClaimsRequests) {
        log.info("Audit mapper by customer approval Mapper");

        return insuranceClaimsRequests.stream()
                .map(se -> {
                    log.info("Mapper from customer Mapper {}", se.getId());
                    ClaimsRequestAuditDTO dto = modelMapper.map(se, ClaimsRequestAuditDTO.class);

                    dto.setRequestStatusDescription(se.getRequestStatus().getDescription());
                    return dto.toString();
                })
                .collect(Collectors.toList());
    }
}

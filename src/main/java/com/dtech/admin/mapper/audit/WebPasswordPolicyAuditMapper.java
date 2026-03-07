/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 2:15 PM
 * <p>
 */

package com.dtech.admin.mapper.audit;
import com.dtech.admin.dto.audit.WebPasswordPolicyAuditDTO;
import com.dtech.admin.dto.response.PasswordPolicyResponseDTO;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class WebPasswordPolicyAuditMapper {
    public static List<String> mapToDTOAudit(List<PasswordPolicyResponseDTO> passwordPolicyResponseDTOS) {
        log.info("Password policy mapper");
        final ModelMapper modelMapper = new ModelMapper();
        return passwordPolicyResponseDTOS.stream()
                .map(se -> {
                    log.info("Mapper from web password policy Mapper {}", se.getId());
                    WebPasswordPolicyAuditDTO passwordPolicyAuditDTO = modelMapper.map(se, WebPasswordPolicyAuditDTO.class);
                    return passwordPolicyAuditDTO.toString();
                })
                .collect(Collectors.toList());
    }
}

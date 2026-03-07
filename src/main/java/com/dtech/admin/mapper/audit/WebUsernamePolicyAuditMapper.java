/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 2:15 PM
 * <p>
 */

package com.dtech.admin.mapper.audit;
import com.dtech.admin.dto.audit.WebUsernamePolicyAuditDTO;
import com.dtech.admin.dto.response.UsernamePolicyResponseDTO;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class WebUsernamePolicyAuditMapper {
    public static List<String> mapToDTOAudit(List<UsernamePolicyResponseDTO> usernamePolicyResponseDTOS) {
        log.info("Username policy mapper");
        final ModelMapper modelMapper = new ModelMapper();
        return usernamePolicyResponseDTOS.stream()
                .map(se -> {
                    log.info("Mapper from web username policy Mapper {}", se.getId());
                    WebUsernamePolicyAuditDTO webUsernamePolicyAuditDTO = modelMapper.map(se, WebUsernamePolicyAuditDTO.class);
                    return webUsernamePolicyAuditDTO.toString();
                })
                .collect(Collectors.toList());
    }
}

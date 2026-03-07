/**
 * User: Himal_J
 * Date: 4/30/2025
 * Time: 2:02 PM
 * <p>
 */

package com.dtech.admin.mapper.audit;

import com.dtech.admin.dto.audit.WebUserAuditDTO;
import com.dtech.admin.model.WebUser;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;
@Log4j2
public class WebUserAuditMapper {
    private static final ModelMapper modelMapper = new ModelMapper();

    public static List<String> mapToDTOAudit(List<WebUser> webUsers) {
        log.info("Audit mapper by web user Mapper");

        return webUsers.stream()
                .map(se -> {
                    log.info("Mapper from Web user Mapper {}", se.getUsername());
                    WebUserAuditDTO dto = modelMapper.map(se, WebUserAuditDTO.class);
                    dto.setStatusDescription(se.getStatus().getDescription());
                    dto.setUserRoleDescription(se.getUserRole().getDescription());
                    return dto.toString();
                })
                .collect(Collectors.toList());
    }
}

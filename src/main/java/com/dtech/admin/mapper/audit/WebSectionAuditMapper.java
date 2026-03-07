/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 2:15 PM
 * <p>
 */

package com.dtech.admin.mapper.audit;

import com.dtech.admin.dto.CommonResponseDTO;
import com.dtech.admin.dto.audit.CommonAuditDTO;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class WebSectionAuditMapper {
    public static List<String> mapToDTOAudit(List<CommonResponseDTO> sectionAuditDTOS) {
        log.info("Audit mapper by WebSectionPageMapper");
        return sectionAuditDTOS.stream()
                .map(se -> {
                    log.info("Mapper from WebSectionPageMapper {}", se.getCode());
                    CommonAuditDTO dto = new CommonAuditDTO();
                    dto.setCode(se.getCode());
                    dto.setDescription(se.getDescription());
                    dto.setStatusDescription(se.getStatusDescription());
                    return dto.toString();
                })
                .collect(Collectors.toList());
    }
}

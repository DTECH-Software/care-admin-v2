package com.dtech.admin.mapper.audit;

import com.dtech.admin.dto.audit.WebUserRolePageTaskAuditDTO;
import com.dtech.admin.model.WebUserRolePageTask;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;
@Log4j2
public class WebUserRolePageTaskAuditMapper {

    public static List<String> mapToDTOAudit(List<WebUserRolePageTask> tasksToAdd) {
        log.info("Audit mapper by WebUserRolePageTaskMapper");
        return tasksToAdd.stream()
                .map(task -> {
                    log.info("Mapper from WebUserRolePageTaskMapper {}", task.getId());
                    WebUserRolePageTaskAuditDTO dto = new WebUserRolePageTaskAuditDTO();
                    dto.setRole(task.getRole().getCode());
                    dto.setPage(task.getPage().getCode());
                    dto.setTask(task.getTask().getCode());
                    return dto.toString();
                })
                .collect(Collectors.toList());
    }
}
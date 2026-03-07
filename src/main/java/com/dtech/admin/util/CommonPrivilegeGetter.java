/**
 * User: Himal_J
 * Date: 4/27/2025
 * Time: 7:36 PM
 * <p>
 */

package com.dtech.admin.util;

import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebUserRolePageTask;
import com.dtech.admin.repository.WebPageRepository;
import com.dtech.admin.repository.WebUserRepository;
import com.dtech.admin.repository.WebUserRolePageTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Log4j2
@Component
@RequiredArgsConstructor
public class CommonPrivilegeGetter {

    @Autowired
    private final WebUserRepository webUserRepository;

    @Autowired
    private final WebUserRolePageTaskRepository webUserRolePageTaskRepository;

    @Autowired
    private final WebPageRepository webPageRepository;

    public AuthorizationTaskResponseDTO  getPrivileges(String username, String page) {
        try {
            log.info("Common privilege get started {} {}", username, page);

           return webUserRepository.findByUsername(username).map( webUser -> {
               return webPageRepository.findByCodeAndStatus(page, Status.ACTIVE).map(webPage -> {
                    List<WebUserRolePageTask> allTask = webUserRolePageTaskRepository.findAllByRoleAndPage(webUser.getUserRole(), webPage);

                   AuthorizationTaskResponseDTO authorizationTaskResponseDTO = new AuthorizationTaskResponseDTO();

                    allTask.forEach(webUserRolePageTask -> {
                        String taskCode = webUserRolePageTask.getTask().getStatus().equals(Status.ACTIVE)
                                ? webUserRolePageTask.getTask().getCode() : "";

                        switch (taskCode) {
                            case "ADD" -> authorizationTaskResponseDTO.setAdd(true);
                            case "UPDATE" -> authorizationTaskResponseDTO.setUpdate(true);
                            case "DELETE" -> authorizationTaskResponseDTO.setDelete(true);
                            case "VIEW" -> authorizationTaskResponseDTO.setView(true);
                            case "SEARCH" -> authorizationTaskResponseDTO.setSearch(true);
                            case "ASSIGNED_UNASSIGNED_TASK_RETRIEVED" -> authorizationTaskResponseDTO.setUserRolePrivilegeAssign(true);
                            case "PASSWORD_RESET" -> authorizationTaskResponseDTO.setPasswordReset(true);
                            case "FILE_UPLOAD" -> authorizationTaskResponseDTO.setFileUpload(true);
                            case "FINANCIAL_APP" -> authorizationTaskResponseDTO.setFinancialApp(true);
                            case "STAFF_CAT_UPDATE" -> authorizationTaskResponseDTO.setStaffCategoryUpdate(true);
                        }
                    });

                    return authorizationTaskResponseDTO;

                }).orElseGet(() -> {
                    log.info("Common privilege get no page {}", page);
                    return null;
                 });
            }).orElseGet(() -> {
                log.info("Common privilege get no user {}", username);
                return null;
            });
        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }

}

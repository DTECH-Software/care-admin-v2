package com.dtech.admin.repository;

import com.dtech.admin.model.WebPage;
import com.dtech.admin.model.WebUserRole;
import com.dtech.admin.model.WebUserRolePageTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebUserRolePageTaskRepository extends JpaRepository<WebUserRolePageTask, Long> {
    List<WebUserRolePageTask> findAllByRole(WebUserRole role);
    List<WebUserRolePageTask> findAllByRoleAndPage(WebUserRole role, WebPage webPage);
    void deleteAllByRoleAndPage(WebUserRole role, WebPage webPage);
}

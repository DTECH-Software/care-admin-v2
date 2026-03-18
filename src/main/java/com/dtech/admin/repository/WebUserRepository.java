package com.dtech.admin.repository;

import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebUserRepository extends JpaRepository<WebUser, Long> , JpaSpecificationExecutor<WebUser> {
    Optional<WebUser> findByUsernameAndLoginStatus(String username, Status loginStatus);
    Optional<WebUser> findByUsernameAndStatus(String username,Status status);
    Optional<WebUser> findByUsernameAndStatusIn(String username,List<Status> statuses);
    Optional<WebUser> findByUsername(String username);
    boolean existsByUsernameEqualsIgnoreCaseAndStatusNot(String username,Status status);
    List<WebUser> findAllByStatus(Status status);
    List<WebUser> findAllByApprovalLevelAndStatus(ApprovalLevel approvalLevel, Status status);
}

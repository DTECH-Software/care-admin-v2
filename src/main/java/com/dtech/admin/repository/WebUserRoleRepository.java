package com.dtech.admin.repository;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebUserRoleRepository extends JpaRepository<WebUserRole, Long> , JpaSpecificationExecutor<WebUserRole> {
    @Query(value = "SELECT new com.dtech.admin.dto.SimpleBaseDTO(s.code,s.description) FROM WebUserRole s WHERE s.status = :status")
    List<SimpleBaseDTO> findAllByWebUserRole(Status status);

    Optional<WebUserRole> findByCodeAndStatus(String code,Status status);
    boolean existsByCodeAndStatusIn(String code,List<Status> status);
}

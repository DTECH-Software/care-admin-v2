package com.dtech.admin.repository;

import com.dtech.admin.model.MaritalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MaritalStatusRepository extends JpaRepository<MaritalStatus, Long> , JpaSpecificationExecutor<MaritalStatus> {
}

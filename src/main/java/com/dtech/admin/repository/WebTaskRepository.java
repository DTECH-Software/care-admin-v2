package com.dtech.admin.repository;

import com.dtech.admin.model.WebTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebTaskRepository extends JpaRepository<WebTask, Long>, JpaSpecificationExecutor<WebTask> {
    Optional<WebTask> findByCode(String code);
}

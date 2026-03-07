package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.CompanyTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyTypeRepository extends JpaRepository<CompanyTypes,Long> {
    Optional<CompanyTypes> findByCodeAndStatus(String code, Status status);
    List<CompanyTypes> findAllByStatus(Status status);
}

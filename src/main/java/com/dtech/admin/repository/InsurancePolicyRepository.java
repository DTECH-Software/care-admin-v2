package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {
    Optional<InsurancePolicy> findByCodeAndStatus(String code, Status status);
    List<InsurancePolicy> findAllByStatus(Status status);
    Optional<InsurancePolicy> findByIdAndStatus(Long id, Status status);
}

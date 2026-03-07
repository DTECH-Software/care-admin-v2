package com.dtech.admin.repository;


import com.dtech.admin.enums.Range;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.DeathBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeathBeneficiaryRepository extends JpaRepository<DeathBeneficiary, Long> {
    Optional<DeathBeneficiary> findByCodeAndRangeAndStatus(com.dtech.admin.enums.DeathBeneficiary deathBeneficiary, Range range, Status status);
    Optional<DeathBeneficiary> findByCodeAndStatus(com.dtech.admin.enums.DeathBeneficiary deathBeneficiary, Status status);
    List<DeathBeneficiary> findAllByStatus(Status status);
}

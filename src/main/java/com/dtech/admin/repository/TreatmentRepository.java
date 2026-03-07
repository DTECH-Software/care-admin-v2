package com.dtech.admin.repository;


import com.dtech.admin.enums.Status;
import com.dtech.admin.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    List<Treatment> findAllByStatus(Status status);
    Optional<Treatment> findByTreatmentCodeAndStatus(String code, Status status);
}

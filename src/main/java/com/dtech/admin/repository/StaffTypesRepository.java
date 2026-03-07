package com.dtech.admin.repository;


import com.dtech.admin.enums.Status;
import com.dtech.admin.model.StaffTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffTypesRepository extends JpaRepository<StaffTypes, Long> {
    Optional<StaffTypes> findByCodeAndStatus(String code, Status status);
    List<StaffTypes> findAllByStatus(Status status);
}

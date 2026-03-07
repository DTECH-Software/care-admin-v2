package com.dtech.admin.repository;


import com.dtech.admin.enums.Status;
import com.dtech.admin.model.StaffCategories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffCategoriesRepository extends JpaRepository<StaffCategories, Long> {
    Optional<StaffCategories> findByCodeAndStatus(String code, Status status);
    List<StaffCategories> findAllByStatus(Status status);
}

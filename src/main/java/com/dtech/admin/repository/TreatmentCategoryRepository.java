/**
 * User: Himal_J
 * Date: 4/6/2025
 * Time: 12:22 PM
 * <p>
 */

package com.dtech.admin.repository;



import com.dtech.admin.enums.Status;
import com.dtech.admin.model.TreatmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentCategoryRepository extends JpaRepository<TreatmentCategory, Long> {
    Optional<TreatmentCategory> findByCodeAndStatus(String code, Status status);
    List<TreatmentCategory> findAllByStatus(Status status);
}

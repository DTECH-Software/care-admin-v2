package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.InsurancePolicyStaffCategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsurancePolicyStaffCategoryGroupRepository extends JpaRepository<InsurancePolicyStaffCategoryGroup, Long> {

    List<InsurancePolicyStaffCategoryGroup> findAllByStatus(Status status);

    Optional<InsurancePolicyStaffCategoryGroup> findByInsurancePolicy_IdAndStaffCategories_CodeAndStatus(
            Long insurancePolicyId,
            String staffCategoryCode,
            Status status
    );

    List<InsurancePolicyStaffCategoryGroup> findAllByMainCategoryCodeIgnoreCaseAndStatus(String mainCategoryCode,
                                                                                          Status status);

    List<InsurancePolicyStaffCategoryGroup> findAllByStaffCategories_CodeAndStatus(String staffCategoryCode,
                                                                                   Status status);
}

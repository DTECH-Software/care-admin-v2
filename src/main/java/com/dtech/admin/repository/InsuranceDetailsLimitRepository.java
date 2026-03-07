/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 10:32 AM
 * <p>
 */

package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.InsuranceDetailsLimit;
import com.dtech.admin.model.InsurancePolicy;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import com.dtech.admin.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceDetailsLimitRepository extends JpaRepository<InsuranceDetailsLimit, Long> {

    Optional<InsuranceDetailsLimit> findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment(InsurancePolicy insurancePolicy,
                                                                                                              Status status,
                                                                                                              InsuranceStaffCategoryPeriod insuranceYear, Treatment treatment);
    List<InsuranceDetailsLimit> findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriod(InsurancePolicy insurancePolicy,Status status,InsuranceStaffCategoryPeriod insuranceYear);
}

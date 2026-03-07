/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 7:58 PM
 * <p>
 */

package com.dtech.admin.repository;


import com.dtech.admin.enums.Facility;
import com.dtech.admin.enums.RelationCategory;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ClaimsDependents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ClaimDependentsRepository extends JpaRepository<ClaimsDependents, Long>, JpaSpecificationExecutor<ClaimsDependents> {
    boolean existsAllByApplicationUserAndRelationCategoryAndStatusIn(ApplicationUser applicationUser, RelationCategory relationCategory, List<Workflow> workflow);
    boolean existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(ApplicationUser applicationUser, RelationCategory relationCategory, List<Workflow> workflow, Long id);
    Optional<ClaimsDependents> findByIdAndApplicationUserAndStatusAndEligibleFacilityIn(Long id, ApplicationUser applicationUser, Workflow status, List<Facility> facility);
    List<ClaimsDependents> findByApplicationUserAndStatusAndEligibleFacilityInAndLiveStatus(ApplicationUser applicationUser, Workflow status, List<Facility> facility,Boolean live);
    long countByStatus(Workflow status);

}

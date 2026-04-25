package com.dtech.admin.repository;


import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.DeathClaimRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeathClaimRequestRepository extends JpaRepository<DeathClaimRequest, Long> , JpaSpecificationExecutor<DeathClaimRequest>  {
    Optional<DeathClaimRequest> findByClaimsDependentsAndEmployeeAndRequestStatusIn(ClaimsDependents claimsDependents, ApplicationUser applicationUser, List<Workflow> workflow);
    boolean existsByClaimsDependentsAndEmployeeAndRequestStatusIn(ClaimsDependents claimsDependents, ApplicationUser applicationUser, List<Workflow> workflow);
    long countByRequestStatus(Workflow status);
    List<DeathClaimRequest> findAllByCreatedDateBetween(Date fromDate, Date toDate);

}

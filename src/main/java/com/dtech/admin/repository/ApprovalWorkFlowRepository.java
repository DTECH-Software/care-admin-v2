package com.dtech.admin.repository;


import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.InsuranceClaimsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalWorkFlowRepository extends JpaRepository<ApprovalWorkFlow, Long> {

}

package com.dtech.admin.repository;

import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.ApprovalWorkflowRejectReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalWorkflowRejectReasonRepository extends JpaRepository<ApprovalWorkflowRejectReason, Long> {
    List<ApprovalWorkflowRejectReason> findAllByApprovalWorkFlowOrderByIdAsc(ApprovalWorkFlow approvalWorkFlow);
}

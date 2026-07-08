package com.dtech.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true, exclude = "approvalWorkFlow")
@ToString(exclude = "approvalWorkFlow")
@Entity
@Table(name = "approval_workflow_reject_reason")
@Data
public class ApprovalWorkflowRejectReason extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_workflow_id", nullable = false)
    private ApprovalWorkFlow approvalWorkFlow;

    @Column(name = "reason_code", nullable = false)
    private String reasonCode;

    @Column(name = "reason_description", nullable = false)
    private String reasonDescription;

    @Column(name = "reason_category")
    private String reasonCategory;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "remark", length = 500)
    private String remark;
}

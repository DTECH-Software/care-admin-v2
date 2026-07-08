package com.dtech.admin.model;

import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.Workflow;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true, exclude = "rejectReasons")
@Entity
@Table(name = "approval_work_flow")
@Data
public class ApprovalWorkFlow extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "approved_level",nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalLevel approvalLevel;

    @Column(name = "approved_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date approvedDate;

    @Column(name = "approved_user")
    private String approvedUser;

    @Column(name = "rejected_remak")
    private String rejectedRemark;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Workflow status;

    @Column(name = "approved_amount",nullable = false)
    private BigDecimal approvedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", referencedColumnName = "id")
    private InsuranceStaffCategoryPeriod policy;

    @OneToMany(mappedBy = "approvalWorkFlow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalWorkflowRejectReason> rejectReasons = new ArrayList<>();

}

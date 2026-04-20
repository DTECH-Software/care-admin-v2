package com.dtech.admin.model;

import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.Workflow;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "claims_request")
@Data
public class InsuranceClaimsRequest extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "request_id", nullable = false, updatable = false, unique = true)
    private String requestId;

    @Column(name = "request_amount", nullable = false)
    private BigDecimal requestAmount;

    @Column(name = "request_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Workflow requestStatus;

    @Column(name = "remark")
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    @JoinColumn(name = "insurance_claims_details",updatable = false)
    private InsuranceClaimsDetails insuranceClaimsDetails;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dependent", updatable = false,referencedColumnName = "id")
    private ClaimsDependents claimsDependents;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "employee", nullable = false, updatable = false,referencedColumnName = "id")
    private ApplicationUser employee;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "insurance_details_limit_id",referencedColumnName = "id")
    private InsuranceDetailsLimit insuranceDetailsLimit;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "insurance_quarter_id",referencedColumnName = "id")
    private InsuranceQuarter insuranceQuarter;

    @ManyToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinTable(
            name = "insurance_claim_approval_work_flow",
            joinColumns = @JoinColumn(name = "insurance_claim_id",referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "approval_work_flow_id",referencedColumnName = "id")
    )
    private List<ApprovalWorkFlow> approvalWorkFlows = new ArrayList<>();

    @Column(name = "approval_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalLevel approvalLevel;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;
}

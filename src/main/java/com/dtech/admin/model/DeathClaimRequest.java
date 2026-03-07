/**
 * User: Himal_J
 * Date: 3/24/2025
 * Time: 7:57 PM
 * <p>
 */

package com.dtech.admin.model;


import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.PaymentType;
import com.dtech.admin.enums.Workflow;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "death_claim_request")
@Data
public class DeathClaimRequest extends Audit implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "request_id", nullable = false, updatable = false, unique = true)
    private String requestId;

    @Column(name = "death_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date deathDate;

    @Column(name = "request_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Workflow requestStatus;

    @Column(name = "remark")
    private String remark;

    @Column(name = "payment_type",nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(name = "utilize_amount",nullable = false)
    private BigDecimal utilizeAmount;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "dependent", updatable = false,referencedColumnName = "id")
    private ClaimsDependents claimsDependents;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "employee", nullable = false, updatable = false,referencedColumnName = "id")
    private ApplicationUser employee;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "death_beneficiary",updatable = false)
    private DeathBeneficiary deathBeneficiary;

    @ManyToMany(cascade = CascadeType.MERGE,fetch = FetchType.LAZY)
    @JoinTable(
            name = "death_claims_document",
            joinColumns = @JoinColumn(name = "death_claims__id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "document_id", referencedColumnName = "id")
    )
    private List<Document> documents = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinTable(
            name = "death_claim_approval_work_flow",
            joinColumns = @JoinColumn(name = "death_claim_id",referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "approval_work_flow_id",referencedColumnName = "id")
    )
    private List<ApprovalWorkFlow> approvalWorkFlows = new ArrayList<>();

    @Column(name = "approval_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalLevel approvalLevel;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;
}

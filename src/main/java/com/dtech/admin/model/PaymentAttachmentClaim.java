package com.dtech.admin.model;

import com.dtech.admin.enums.PaymentAttachmentClaimState;
import com.dtech.admin.enums.Workflow;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true, exclude = {"paymentAttachment", "insuranceClaimsRequest"})
@ToString(exclude = {"paymentAttachment", "insuranceClaimsRequest"})
@Entity
@Table(name = "payment_attachment_claim")
@Data
public class PaymentAttachmentClaim extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_attachment_id", nullable = false, updatable = false)
    private PaymentAttachment paymentAttachment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_claim_id", nullable = false, updatable = false)
    private InsuranceClaimsRequest insuranceClaimsRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private PaymentAttachmentClaimState state;

    // Snapshot fields
    @Column(name = "request_id", nullable = false, updatable = false)
    private String requestId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "epf")
    private String epf;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "staff_category_code")
    private String staffCategoryCode;

    @Column(name = "treatment_category")
    private String treatmentCategory;

    @Column(name = "claim_category")
    private String claimCategory;

    @Column(name = "request_amount", precision = 16, scale = 2)
    private BigDecimal requestAmount;

    @Column(name = "approved_amount", precision = 16, scale = 2)
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_status")
    private Workflow claimStatus;

    @Column(name = "remark")
    private String remark;
}

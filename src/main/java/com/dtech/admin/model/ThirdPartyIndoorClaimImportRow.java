package com.dtech.admin.model;

import com.dtech.admin.enums.ThirdPartyIndoorClaimClaimantType;
import com.dtech.admin.enums.ThirdPartyIndoorClaimRowStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true, exclude = {"batch", "insuranceClaim"})
@ToString(exclude = {"batch", "insuranceClaim"})
@Entity
@Table(name = "third_party_indoor_claim_batch_row")
@Data
public class ThirdPartyIndoorClaimImportRow extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ThirdPartyIndoorClaimImportBatch batch;

    @Column(name = "row_no", nullable = false)
    private Integer rowNo;

    @Column(name = "external_reference_no", nullable = false)
    private String externalReferenceNo;

    @Column(name = "claimant_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ThirdPartyIndoorClaimClaimantType claimantType;

    @Column(name = "epf_no", nullable = false)
    private String epfNo;

    @Column(name = "employee_nic", nullable = false)
    private String employeeNic;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "dependent_nic")
    private String dependentNic;

    @Column(name = "dependent_name")
    private String dependentName;

    @Column(name = "dependent_relation")
    private String dependentRelation;

    @Column(name = "from_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date fromDate;

    @Column(name = "to_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date toDate;

    @Column(name = "hospital")
    private String hospital;

    @Column(name = "disease", nullable = false)
    private String disease;

    @Column(name = "request_amount", nullable = false)
    private BigDecimal requestAmount;

    @Column(name = "approved_amount", nullable = false)
    private BigDecimal approvedAmount;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ThirdPartyIndoorClaimRowStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_claim_id")
    private InsuranceClaimsRequest insuranceClaim;
}


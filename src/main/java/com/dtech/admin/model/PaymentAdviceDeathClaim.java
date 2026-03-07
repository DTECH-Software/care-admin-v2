package com.dtech.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true, exclude = {"paymentAdvice", "deathClaim"})
@ToString(exclude = {"paymentAdvice", "deathClaim"})
@Entity
@Table(name = "payment_advice_death_claim",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"death_claim_id"})})
@Data
public class PaymentAdviceDeathClaim extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_advice_id", nullable = false, updatable = false)
    private PaymentAdvice paymentAdvice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "death_claim_id", nullable = false, updatable = false)
    private DeathClaimRequest deathClaim;

    @Column(name = "request_id", nullable = false, updatable = false)
    private String requestId;

    @Column(name = "approved_amount", precision = 16, scale = 2)
    private BigDecimal approvedAmount;
}

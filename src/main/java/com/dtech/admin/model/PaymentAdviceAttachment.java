package com.dtech.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true, exclude = {"paymentAdvice", "paymentAttachment"})
@ToString(exclude = {"paymentAdvice", "paymentAttachment"})
@Entity
@Table(name = "payment_advice_attachment",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"payment_attachment_id"})})
@Data
public class PaymentAdviceAttachment extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_advice_id", nullable = false, updatable = false)
    private PaymentAdvice paymentAdvice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_attachment_id", nullable = false, updatable = false)
    private PaymentAttachment paymentAttachment;

    @Column(name = "attachment_no", nullable = false, updatable = false)
    private String attachmentNo;

    @Column(name = "request_amount", precision = 16, scale = 2)
    private BigDecimal requestAmount;

    @Column(name = "approved_amount", precision = 16, scale = 2)
    private BigDecimal approvedAmount;
}

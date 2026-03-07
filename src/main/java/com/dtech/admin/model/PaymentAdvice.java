package com.dtech.admin.model;

import com.dtech.admin.enums.PaymentAdviceStatus;
import com.dtech.admin.enums.PaymentAdviceType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true, exclude = {"attachments"})
@ToString(exclude = {"attachments"})
@Entity
@Table(name = "payment_advice")
@Data
public class PaymentAdvice extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "advice_no", nullable = false, unique = true, updatable = false)
    private String adviceNo;

    @Column(name = "advice_year_start", nullable = false, updatable = false)
    private Integer adviceYearStart;

    @Column(name = "advice_year_end", nullable = false, updatable = false)
    private Integer adviceYearEnd;

    @Column(name = "advice_sequence", nullable = false, updatable = false)
    private Integer adviceSequence;

    @Column(name = "voucher_no", nullable = false, unique = true, updatable = false)
    private String voucherNo;

    @Column(name = "voucher_sequence", nullable = false, updatable = false)
    private Integer voucherSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "advice_type")
    private PaymentAdviceType type;

    @Column(name = "company_code", nullable = false)
    private String companyCode;

    @Column(name = "staff_category_code", nullable = false)
    private String staffCategoryCode;

    @Column(name = "department")
    private String department;

    @Column(name = "insurance")
    private String insurance;

    @Column(name = "total_requested_amount", precision = 16, scale = 2)
    private BigDecimal totalRequestedAmount;

    @Column(name = "total_approved_amount", precision = 16, scale = 2)
    private BigDecimal totalApprovedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentAdviceStatus status;

    @OneToMany(mappedBy = "paymentAdvice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentAdviceAttachment> attachments = new ArrayList<>();
}

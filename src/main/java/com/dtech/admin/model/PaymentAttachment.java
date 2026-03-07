package com.dtech.admin.model;

import com.dtech.admin.enums.PaymentAttachmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true, exclude = {"claims"})
@ToString(exclude = {"claims"})
@Entity
@Table(name = "payment_attachment")
@Data
public class PaymentAttachment extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "attachment_no", nullable = false, unique = true, updatable = false)
    private String attachmentNo;

    @Column(name = "attachment_prefix", nullable = false, updatable = false)
    private String attachmentPrefix;

    @Column(name = "attachment_year", nullable = false, updatable = false)
    private Integer attachmentYear;

    @Column(name = "attachment_sequence", nullable = false, updatable = false)
    private Integer attachmentSequence;

    @Column(name = "notes")
    private String notes;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "staff_category_code")
    private String staffCategoryCode;

    @Column(name = "treatment_category")
    private String treatmentCategory;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_from")
    private Date dateFrom;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_to")
    private Date dateTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentAttachmentStatus status;

    @OneToMany(mappedBy = "paymentAttachment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentAttachmentClaim> claims = new ArrayList<>();
}

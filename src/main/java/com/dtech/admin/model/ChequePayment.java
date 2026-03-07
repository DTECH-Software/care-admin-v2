package com.dtech.admin.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true, exclude = {"documents", "months"})
@ToString(exclude = {"documents", "months"})
@Entity
@Table(name = "cheque_payment")
@Data
public class ChequePayment extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "company_code", nullable = false)
    private String companyCode;

    @Column(name = "staff_category_code", nullable = false)
    private String staffCategoryCode;

    @Column(name = "cheque_year", nullable = false)
    private String year;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "cheque_payment_month", joinColumns = @JoinColumn(name = "cheque_payment_id"))
    @Column(name = "month", nullable = false)
    private List<String> months = new ArrayList<>();

    @Column(name = "cheque_no", nullable = false)
    private String chequeNo;

    @Column(name = "cheque_bank")
    private String chequeBank;

    @Column(name = "cheque_branch")
    private String chequeBranch;

    @Temporal(TemporalType.DATE)
    @Column(name = "cheque_date")
    private Date chequeDate;

    @Column(name = "amount", precision = 16, scale = 2)
    private BigDecimal amount;

    @Temporal(TemporalType.DATE)
    @Column(name = "received_date")
    private Date receivedDate;

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinTable(
            name = "cheque_payment_document",
            joinColumns = @JoinColumn(name = "cheque_payment_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "document_id", referencedColumnName = "id")
    )
    private List<Document> documents = new ArrayList<>();
}

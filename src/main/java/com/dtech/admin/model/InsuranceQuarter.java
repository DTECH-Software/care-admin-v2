package com.dtech.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "insurance_quarter")
@Data
public class InsuranceQuarter extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_details_id",referencedColumnName = "id")
    private InsuranceDetailsLimit insuranceDetailsLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_category_code",referencedColumnName = "code")
    private TreatmentCategory treatmentCategory;

    @Column(name = "quarter_limit",nullable = false)
    private BigDecimal quarterLimit;

    @Column(name = "from_date",nullable = false)
    @Temporal(TemporalType.DATE)
    private Date fromDate;

    @Column(name = "to_date",nullable = false)
    @Temporal(TemporalType.DATE)
    private Date toDate;

}

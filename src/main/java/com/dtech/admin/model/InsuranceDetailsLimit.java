package com.dtech.admin.model;

import com.dtech.admin.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "insurance_details_limit")
@Data
public class InsuranceDetailsLimit extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "global_limit",nullable = false)
    private BigDecimal globalLimit;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "insurance_policy",nullable = false,referencedColumnName = "code")
    private InsurancePolicy insurancePolicy;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "treatment",nullable = false,referencedColumnName = "code")
    private Treatment treatment;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "is_quarter",nullable = false)
    private Boolean isQuarter;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "insurance_staff_category_period",nullable = false,referencedColumnName = "id")
    private InsuranceStaffCategoryPeriod insuranceStaffCategoryPeriod;

    @OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL,mappedBy = "insuranceDetailsLimit")
    private List<InsuranceQuarter> insuranceQuarters;
}

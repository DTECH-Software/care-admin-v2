/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 8:16 AM
 * <p>
 */

package com.dtech.admin.model;


import com.dtech.admin.enums.Facility;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_company_details")
@Data
public class UserCompanyDetails extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "company_type",nullable = false,referencedColumnName = "code")
    private CompanyTypes companyTypes;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_company",referencedColumnName = "code")
    private CompanyTypes paymentCompany;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "death_payment_company",referencedColumnName = "code")
    private CompanyTypes deathPaymentCompany;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "staff_category",nullable = false,referencedColumnName = "code")
    private StaffCategories staffCategories;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_staff_category", referencedColumnName = "code")
    private StaffCategories previousStaffCategories;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "staff_type",nullable = false,referencedColumnName = "code")
    private StaffTypes staffTypes;

    @Column(name = "designation",nullable = false)
    private String designation;

    @Column(name = "permanent_date",nullable = false)
    @Temporal(TemporalType.DATE)
    private Date permanentDate;

    @Column(name = "previous_permanent_date")
    @Temporal(TemporalType.DATE)
    private Date previousPermanentDate;

    @Column(name = "terminate_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date terminateDate;

    @OneToOne(mappedBy = "userCompanyDetails")
    private UserPersonalDetails userCompanyDetails;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "insurance_policy",referencedColumnName = "code")
    private InsurancePolicy insurancePolicy;

    @Column(name = "facility",nullable = false)
    @Enumerated(EnumType.STRING)
    private Facility facility;

    @JoinColumn(name = "promo_doc",nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    private Document promoDocs;

    @Column(name = "transfer_date")
    @Temporal(TemporalType.DATE)
    private Date transferDate;

    @JoinColumn(name = "transfer_doc",nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    private Document transferDocs;

}

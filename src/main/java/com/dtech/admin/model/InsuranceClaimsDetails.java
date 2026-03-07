/**
 * User: Himal_J
 * Date: 3/3/2025
 * Time: 10:56 AM
 * <p>
 */

package com.dtech.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "insurance_claims_details")
@Data
@ToString(exclude = "documents")
public class InsuranceClaimsDetails extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "treatment", nullable = false,referencedColumnName = "code")
    private Treatment treatment;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "treatment_category", nullable = false,referencedColumnName = "code")
    private TreatmentCategory treatmentCategory;

    @Column(name = "from_treatment_date")
    @Temporal(TemporalType.DATE)
    private Date fromTreatmentDate;

    @Column(name = "to_treatment_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date toTreatmentDate;

    @Column(name = "disease")
    private String disease;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinTable(
            name = "insurance_claims_details_document",
            joinColumns = @JoinColumn(name = "insurance_claims_details_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "document_id", referencedColumnName = "id")
    )
    private List<Document> documents = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "insurance_staff_category_period", updatable = false,referencedColumnName = "id")
    private InsuranceStaffCategoryPeriod insuranceStaffCategoryPeriod;

}

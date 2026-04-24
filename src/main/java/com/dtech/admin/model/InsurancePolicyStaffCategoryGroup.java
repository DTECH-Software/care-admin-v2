package com.dtech.admin.model;

import com.dtech.admin.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "insurance_policy_staff_category_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_insurance_policy_staff_category_group_policy_staff",
                columnNames = {"insurance_policy", "staff_category"}))
@Data
public class InsurancePolicyStaffCategoryGroup extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_policy", nullable = false, referencedColumnName = "id")
    private InsurancePolicy insurancePolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_category", nullable = false, referencedColumnName = "code")
    private StaffCategories staffCategories;

    @Column(name = "main_category_code", nullable = false)
    private String mainCategoryCode;

    @Column(name = "main_category_description", nullable = false)
    private String mainCategoryDescription;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
}

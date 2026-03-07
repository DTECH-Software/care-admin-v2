/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 12:02 PM
 * <p>
 */

package com.dtech.admin.model;

import com.dtech.admin.enums.*;
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
@Table(name = "claims_dependents")
@Data
@ToString(exclude = "documents")
public class ClaimsDependents extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "dependent_category",nullable = false)
    @Enumerated(EnumType.STRING)
    private DependentCategory dependentCategory;

    @Column(name = "initials",nullable = false)
    private String initials;

    @Column(name = "first_name",nullable = false)
    private String firstName;

    @Column(name = "last_name",nullable = false)
    private String lastName;

    @Column(name = "dob",nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dob;

    @Column(name = "gender",nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "nic")
    private String nic;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "eligible_facility",nullable = false)
    @Enumerated(EnumType.STRING)
    private Facility eligibleFacility;

    @Column(name = "relation_category",nullable = false)
    @Enumerated(EnumType.STRING)
    private RelationCategory relationCategory;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Workflow status;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "application_user",nullable = false)
    private ApplicationUser applicationUser;

    @Column(name = "live_status",nullable = false,columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean liveStatus;

    @ManyToMany
    @JoinTable(
            name = "claims_dependents_document",
            joinColumns = @JoinColumn(name = "claims_dependents_id",referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "document_id",referencedColumnName = "id")
    )
    private List<Document> documents = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "married",referencedColumnName = "id")
    private Married married;

    @Column(name = "approved_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date approvedDate;

    @Column(name = "approved_user")
    private String approvedUser;

    @Column(name = "remark")
    private String remark;

}

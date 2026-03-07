/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 8:58 AM
 * <p>
 */

package com.dtech.admin.model;


import com.dtech.admin.enums.Gender;
import com.dtech.admin.enums.MaritalStatus;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.Title;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_personal_details")
@Data
public class UserPersonalDetails extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "epf_no",nullable = false,updatable = false)
    private String epfNo;

    @Column(name = "title",nullable = false)
    @Enumerated(EnumType.STRING)
    private Title title;

    @Column(name = "initials",nullable = false,length = 30)
    private String initials;

    @Column(name = "first_name",nullable = false,length = 30)
    private String firstName;

    @Column(name = "last_name",nullable = false,length = 30)
    private String lastName;

    @Column(name = "nic",nullable = false)
    private String nic;

    @Column(name = "email",nullable = false)
    private String email;

    @Column(name = "mobile_no",nullable = false)
    private String mobileNo;

    @Column(name = "gender",nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "marital_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private MaritalStatus maritalStatus;

    @Column(name = "dob",nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dob;

    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "marital_status_id",referencedColumnName = "id")
    private com.dtech.admin.model.MaritalStatus maritalDetails;

    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "user_address",nullable = false,referencedColumnName = "id")
    private UserAddress userAddress;

    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_company_details",nullable = false,referencedColumnName = "id")
    private UserCompanyDetails userCompanyDetails;

    @Column(name = "user_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status userStatus;

    @Column(name = "is_temp",nullable = false)
    private Boolean isTemp;

    @Column(name = "temp_id")
    private String tempId;

    @OneToOne(fetch = FetchType.EAGER,cascade = CascadeType.REFRESH,targetEntity = Document.class)
    @JoinColumn(name = "birth_img",referencedColumnName = "id")
    private Document birthImg;

}

/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 8:11 PM
 * <p>
 */

package com.dtech.admin.model;


import com.dtech.admin.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "application_user")
@Data
@ToString(exclude = "profileImg")
public class ApplicationUser extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "username",nullable = false,updatable = false,unique = true)
    private String username;

    @Column(name = "password",nullable = false)
    @Lob
    private String password;

    @Column(name = "primary_email",unique = true,nullable = false)
    private String primaryEmail;

    @Column(name = "primary_mobile",nullable = false)
    private String primaryMobile;

    @Column(name = "login_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status loginStatus;

    @Column(name = "is_reset",nullable = false)
    private boolean isReset;

    @Column(name = "user_key",nullable = false,updatable = false)
    @Lob
    private String userKey;

//    @Column(name = "last_logged_channel")
//    @Enumerated(EnumType.STRING)
//    private Channe lastLoggedChannel;

    @Column(name = "last_password_change_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastPasswordChangeDate;

    @Column(name = "last_logged_date",nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoggedDate;

    @Column(name = "mb_last_logged_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date mbLastLoggedDate;

    @Column(name = "op_last_logged_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date opLastLoggedDate;

    @Column(name = "expecting_first_time_loging",nullable = false)
    private boolean expectingFirstTimeLogging;

    @Column(name = "expecting_dependents_register",nullable = false)
    private boolean expectingDependentsRegister;

    @Column(name = "password_expired_date",nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date passwordExpiredDate;

    @Column(name = "attempt_count",nullable = false)
    private int attemptCount;

    @Column(name = "otp_attempt_count")
    private int otpAttemptCount;

//    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JoinColumn(name = "otp_session",referencedColumnName = "id")
//    private ApplicationOtpSession applicationOtpSession;

    @Column(name = "otp_attempt_reset_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date otpAttemptResetTime;

//    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
//    @JoinColumn(name = "onboarding_request",referencedColumnName = "id",nullable = false)
//    private OnboardingRequest onboardingRequest;

    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "user_personal_details",referencedColumnName = "id",nullable = false)
    private UserPersonalDetails userPersonalDetails;

//    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
//    @JoinColumn(name = "device_id",referencedColumnName = "id")
//    private ApplicationUserDeviceDetails applicationUserDeviceDetails;

    @OneToMany(mappedBy = "applicationUser")
    private List<ClaimsDependents> claimsDependents;

    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_img",referencedColumnName = "id")
    private Document profileImg;

}

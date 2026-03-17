/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 8:35 AM
 * <p>
 */

package com.dtech.admin.model;

import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "web_user")
@Data
public class WebUser extends AdminAudit implements Serializable , UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "username", nullable = false, updatable = false)
    private String username;

    @Column(name = "password", nullable = false)
    @Lob
    private String password;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "mobile", nullable = false)
    private String mobile;

    @Column(name = "login_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status loginStatus;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "is_reset", nullable = false)
    private boolean isReset;

    @Column(name = "first_name",nullable = false,length = 30)
    private String firstName;

    @Column(name = "last_name",nullable = false,length = 30)
    private String lastName;

    @Column(name = "nic",nullable = false)
    private String nic;

    @Column(name = "user_key", nullable = false, updatable = false)
    @Lob
    private String userKey;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "web_token", referencedColumnName = "id")
    private WebToken webToken;

    @Column(name = "last_password_change_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastPasswordChangeDate;

    @Column(name = "last_logged_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoggedDate;

    @Column(name = "expecting_first_time_loging", nullable = false)
    private boolean expectingFirstTimeLogging;

    @Column(name = "password_expired_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date passwordExpiredDate;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = Document.class)
    @JoinColumn(name = "profile_img", referencedColumnName = "id")
    private Document profileImg;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = WebUserRole.class)
    @JoinColumn(name = "user_role", referencedColumnName = "code")
    private WebUserRole userRole;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "web_user_company",
            joinColumns = @JoinColumn(name = "web_user_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<CompanyTypes> companies = new LinkedHashSet<>();

    @Column(name = "approval_level")
    @Enumerated(EnumType.STRING)
    private ApprovalLevel approvalLevel;

    @Column(name = "death_approval_level")
    @Enumerated(EnumType.STRING)
    private ApprovalLevel deathApprovalLevel;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> userRole.getCode());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

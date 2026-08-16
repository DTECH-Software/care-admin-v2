package com.dtech.admin.model;

import com.dtech.admin.enums.EmailCompanyScope;
import com.dtech.admin.enums.EmailRecipientType;
import com.dtech.admin.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "email_notification_recipient_rule")
@Data
public class EmailNotificationRecipientRule extends AdminAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EmailNotificationEvent event;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 30)
    private EmailRecipientType recipientType;

    @Column(name = "recipient_code", nullable = false, length = 100)
    private String recipientCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_scope", nullable = false, length = 30)
    private EmailCompanyScope companyScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;
}

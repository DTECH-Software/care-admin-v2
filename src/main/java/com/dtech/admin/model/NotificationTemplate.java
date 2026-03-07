package com.dtech.admin.model;

import com.dtech.admin.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "notification_template")
@Data
public class NotificationTemplate extends AdminAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "type",nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Column(name = "title",nullable = false)
    private String title;

    @Column(name = "message_body")
    private String messageBody;

    @Column(name = "email_body")
    private String emailBody;

}

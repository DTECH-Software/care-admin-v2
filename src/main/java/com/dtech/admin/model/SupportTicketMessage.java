package com.dtech.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true, exclude = "ticket")
@ToString(exclude = "ticket")
@Entity
@Table(name = "support_ticket_message")
@Data
public class SupportTicketMessage extends AdminAudit implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(name = "author_username", nullable = false, length = 100)
    private String authorUsername;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Lob
    @Column(name = "message", nullable = false)
    private String message;
}

package com.dtech.admin.model;

import com.dtech.admin.enums.SupportTicketStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true, exclude = "ticket")
@ToString(exclude = "ticket")
@Entity
@Table(name = "support_ticket_status_history")
@Data
public class SupportTicketStatusHistory extends AdminAudit implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private SupportTicketStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private SupportTicketStatus newStatus;

    @Column(name = "remark", length = 1000)
    private String remark;
}

package com.dtech.admin.model;

import com.dtech.admin.enums.SupportTicketPriority;
import com.dtech.admin.enums.SupportTicketStatus;
import com.dtech.admin.enums.SupportTicketSystemType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true, exclude = {"messages", "attachments", "statusHistory"})
@ToString(exclude = {"messages", "attachments", "statusHistory"})
@Entity
@Table(name = "support_ticket")
@Data
public class SupportTicket extends AdminAudit implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_no", nullable = false, unique = true, updatable = false, length = 40)
    private String ticketNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type", nullable = false, updatable = false, length = 30)
    private SupportTicketSystemType systemType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyTypes company;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Lob
    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private SupportTicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SupportTicketStatus status;

    @Lob
    @Column(name = "resolution")
    private String resolution;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "resolved_date")
    private Date resolvedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "closed_date")
    private Date closedDate;

    @Version
    @Column(name = "record_version", nullable = false)
    private Long recordVersion = 0L;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdDate ASC")
    private List<SupportTicketMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdDate ASC")
    private List<SupportTicketAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdDate ASC")
    private List<SupportTicketStatusHistory> statusHistory = new ArrayList<>();
}

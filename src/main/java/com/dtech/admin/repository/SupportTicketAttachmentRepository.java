package com.dtech.admin.repository;

import com.dtech.admin.model.SupportTicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketAttachmentRepository extends JpaRepository<SupportTicketAttachment, Long> {
}

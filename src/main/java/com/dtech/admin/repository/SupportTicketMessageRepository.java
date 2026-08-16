package com.dtech.admin.repository;

import com.dtech.admin.model.SupportTicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, Long> {
}

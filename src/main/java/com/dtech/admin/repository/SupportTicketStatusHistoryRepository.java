package com.dtech.admin.repository;

import com.dtech.admin.model.SupportTicketStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketStatusHistoryRepository extends JpaRepository<SupportTicketStatusHistory, Long> {
}

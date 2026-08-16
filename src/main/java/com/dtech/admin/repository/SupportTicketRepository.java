package com.dtech.admin.repository;

import com.dtech.admin.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {
}

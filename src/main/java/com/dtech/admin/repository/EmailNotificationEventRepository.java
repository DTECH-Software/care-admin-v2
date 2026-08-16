package com.dtech.admin.repository;

import com.dtech.admin.model.EmailNotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailNotificationEventRepository extends JpaRepository<EmailNotificationEvent, Long> {
    Optional<EmailNotificationEvent> findByCode(String code);
}

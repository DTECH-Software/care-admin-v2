package com.dtech.admin.repository;

import com.dtech.admin.enums.MessageType;
import com.dtech.admin.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByType(MessageType title);
}
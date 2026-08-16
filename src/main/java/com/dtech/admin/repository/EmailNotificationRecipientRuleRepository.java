package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.EmailNotificationRecipientRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailNotificationRecipientRuleRepository extends JpaRepository<EmailNotificationRecipientRule, Long> {
    List<EmailNotificationRecipientRule> findAllByEvent_CodeAndEvent_StatusAndStatus(
            String eventCode, Status eventStatus, Status ruleStatus);
}

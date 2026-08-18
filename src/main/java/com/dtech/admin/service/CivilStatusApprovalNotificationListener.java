package com.dtech.admin.service;

import com.dtech.admin.enums.CivilStatusEmailEvent;
import com.dtech.admin.event.CivilStatusApprovedEvent;
import com.dtech.admin.model.MaritalStatus;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.MaritalStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@Log4j2
@RequiredArgsConstructor
public class CivilStatusApprovalNotificationListener {

    private final MaritalStatusRepository maritalStatusRepository;
    private final CivilStatusEmailRecipientService civilStatusEmailRecipientService;
    private final EmailNotificationService emailNotificationService;

    @Async
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyAdminTeam(CivilStatusApprovedEvent event) {
        try {
            MaritalStatus civilStatusRequest = maritalStatusRepository
                    .findById(event.civilStatusRequestId())
                    .orElse(null);
            if (civilStatusRequest == null) {
                log.warn("Skipping civil status approval email. Request {} was not found",
                        event.civilStatusRequestId());
                return;
            }

            List<WebUser> recipients = civilStatusEmailRecipientService.resolve(
                    CivilStatusEmailEvent.CIVIL_STATUS_APPROVED, civilStatusRequest);
            emailNotificationService.notifyCivilStatusApprovedByHr(
                    recipients, civilStatusRequest, event.approvedBy());
        } catch (Exception ex) {
            // Email delivery must never change an already committed approval.
            log.error("Failed to process civil status approval email for request {}",
                    event.civilStatusRequestId(), ex);
        }
    }
}

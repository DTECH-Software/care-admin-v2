package com.dtech.admin.service;

import com.dtech.admin.enums.DependentEmailEvent;
import com.dtech.admin.event.DependentApprovedEvent;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.ClaimDependentsRepository;
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
public class DependentApprovalNotificationListener {

    private final ClaimDependentsRepository claimDependentsRepository;
    private final DependentEmailRecipientService dependentEmailRecipientService;
    private final EmailNotificationService emailNotificationService;

    @Async
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyAdminTeam(DependentApprovedEvent event) {
        try {
            ClaimsDependents dependent = claimDependentsRepository.findById(event.dependentId())
                    .orElse(null);
            if (dependent == null) {
                log.warn("Skipping dependent approval email. Dependent {} was not found", event.dependentId());
                return;
            }

            List<WebUser> recipients = dependentEmailRecipientService.resolve(
                    DependentEmailEvent.DEPENDENT_APPROVED, dependent);
            emailNotificationService.notifyDependentApprovedByHr(recipients, dependent, event.approvedBy());
        } catch (Exception ex) {
            // Notification delivery must never change an already committed approval.
            log.error("Failed to process dependent approval email for dependent {}", event.dependentId(), ex);
        }
    }
}

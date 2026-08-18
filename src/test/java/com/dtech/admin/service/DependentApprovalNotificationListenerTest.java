package com.dtech.admin.service;

import com.dtech.admin.enums.DependentEmailEvent;
import com.dtech.admin.event.DependentApprovedEvent;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.ClaimDependentsRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class DependentApprovalNotificationListenerTest {

    private final ClaimDependentsRepository claimDependentsRepository = mock(ClaimDependentsRepository.class);
    private final DependentEmailRecipientService recipientService = mock(DependentEmailRecipientService.class);
    private final EmailNotificationService emailNotificationService = mock(EmailNotificationService.class);
    private final DependentApprovalNotificationListener listener = new DependentApprovalNotificationListener(
            claimDependentsRepository, recipientService, emailNotificationService);

    @Test
    void sendsConfiguredEmailForCommittedApprovalEvent() {
        ClaimsDependents dependent = new ClaimsDependents();
        dependent.setId(15L);
        WebUser recipient = new WebUser();
        when(claimDependentsRepository.findById(15L)).thenReturn(Optional.of(dependent));
        when(recipientService.resolve(DependentEmailEvent.DEPENDENT_APPROVED, dependent))
                .thenReturn(List.of(recipient));

        listener.notifyAdminTeam(new DependentApprovedEvent(15L, "hr-user"));

        verify(emailNotificationService)
                .notifyDependentApprovedByHr(List.of(recipient), dependent, "hr-user");
    }

    @Test
    void skipsEmailWhenDependentNoLongerExists() {
        when(claimDependentsRepository.findById(99L)).thenReturn(Optional.empty());

        listener.notifyAdminTeam(new DependentApprovedEvent(99L, "hr-user"));

        verifyNoInteractions(recipientService, emailNotificationService);
    }
}

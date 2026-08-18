package com.dtech.admin.service;

import com.dtech.admin.enums.CivilStatusEmailEvent;
import com.dtech.admin.event.CivilStatusApprovedEvent;
import com.dtech.admin.model.MaritalStatus;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.MaritalStatusRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class CivilStatusApprovalNotificationListenerTest {

    private final MaritalStatusRepository maritalStatusRepository = mock(MaritalStatusRepository.class);
    private final CivilStatusEmailRecipientService recipientService = mock(CivilStatusEmailRecipientService.class);
    private final EmailNotificationService emailNotificationService = mock(EmailNotificationService.class);
    private final CivilStatusApprovalNotificationListener listener = new CivilStatusApprovalNotificationListener(
            maritalStatusRepository, recipientService, emailNotificationService);

    @Test
    void sendsConfiguredEmailForCommittedApprovalEvent() {
        MaritalStatus request = new MaritalStatus();
        request.setId(12L);
        WebUser recipient = new WebUser();
        when(maritalStatusRepository.findById(12L)).thenReturn(Optional.of(request));
        when(recipientService.resolve(CivilStatusEmailEvent.CIVIL_STATUS_APPROVED, request))
                .thenReturn(List.of(recipient));

        listener.notifyAdminTeam(new CivilStatusApprovedEvent(12L, "hr-user"));

        verify(emailNotificationService)
                .notifyCivilStatusApprovedByHr(List.of(recipient), request, "hr-user");
    }

    @Test
    void skipsEmailWhenRequestNoLongerExists() {
        when(maritalStatusRepository.findById(99L)).thenReturn(Optional.empty());

        listener.notifyAdminTeam(new CivilStatusApprovedEvent(99L, "hr-user"));

        verifyNoInteractions(recipientService, emailNotificationService);
    }
}

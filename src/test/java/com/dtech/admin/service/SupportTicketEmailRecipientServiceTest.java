package com.dtech.admin.service;

import com.dtech.admin.enums.*;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.EmailNotificationEventRepository;
import com.dtech.admin.repository.EmailNotificationRecipientRuleRepository;
import com.dtech.admin.repository.WebUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketEmailRecipientServiceTest {
    @Mock private EmailNotificationEventRepository eventRepository;
    @Mock private EmailNotificationRecipientRuleRepository ruleRepository;
    @Mock private WebUserRepository webUserRepository;
    @InjectMocks private SupportTicketEmailRecipientService service;

    @Test
    void updatedTicketNotifiesCreatorAndOtherSameCompanySupportUsersButNotActor() {
        EmailNotificationEvent event = event(Status.ACTIVE);
        EmailNotificationRecipientRule supportRule = rule(event, EmailRecipientType.USER_ROLE,
                "SUPERADMIN", EmailCompanyScope.SAME_COMPANY_OR_UNASSIGNED);
        EmailNotificationRecipientRule creatorRule = rule(event, EmailRecipientType.EVENT_USER,
                SupportTicketEmailRecipientService.TICKET_CREATOR, EmailCompanyScope.ALL_COMPANIES);
        CompanyTypes sgcs = company("SGCS");
        CompanyTypes other = company("OTHER");
        WebUser actor = user(1L, "support.actor", "actor@test.lk", sgcs);
        WebUser support = user(2L, "support.other", "support@test.lk", sgcs);
        WebUser wrongCompany = user(3L, "support.wrong", "wrong@test.lk", other);
        WebUser creator = user(4L, "ticket.owner", "owner@test.lk", sgcs);
        SupportTicket ticket = new SupportTicket();
        ticket.setCompany(sgcs);
        ticket.setCreatedBy("ticket.owner");

        when(eventRepository.findByCode(SupportTicketEmailEvent.SUPPORT_TICKET_UPDATED.name()))
                .thenReturn(Optional.of(event));
        when(ruleRepository.findAllByEvent_CodeAndEvent_StatusAndStatus(
                SupportTicketEmailEvent.SUPPORT_TICKET_UPDATED.name(), Status.ACTIVE, Status.ACTIVE))
                .thenReturn(List.of(supportRule, creatorRule));
        when(webUserRepository.findAllByUserRole_CodeIgnoreCaseAndStatus("SUPERADMIN", Status.ACTIVE))
                .thenReturn(List.of(actor, support, wrongCompany));
        when(webUserRepository.findByUsernameIgnoreCaseAndStatus("ticket.owner", Status.ACTIVE))
                .thenReturn(Optional.of(creator));

        List<WebUser> recipients = service.resolve(SupportTicketEmailEvent.SUPPORT_TICKET_UPDATED,
                ticket, "support.actor");

        assertEquals(List.of("support.other", "ticket.owner"),
                recipients.stream().map(WebUser::getUsername).toList());
    }

    @Test
    void inactiveEventSendsNoEmail() {
        when(eventRepository.findByCode(SupportTicketEmailEvent.SUPPORT_TICKET_RESOLVED.name()))
                .thenReturn(Optional.of(event(Status.INACTIVE)));

        assertEquals(List.of(), service.resolve(SupportTicketEmailEvent.SUPPORT_TICKET_RESOLVED,
                new SupportTicket(), "support.actor"));
        verifyNoInteractions(ruleRepository, webUserRepository);
    }

    private EmailNotificationEvent event(Status status) {
        EmailNotificationEvent event = new EmailNotificationEvent();
        event.setId(1L);
        event.setCode(SupportTicketEmailEvent.SUPPORT_TICKET_UPDATED.name());
        event.setStatus(status);
        return event;
    }

    private EmailNotificationRecipientRule rule(EmailNotificationEvent event, EmailRecipientType type,
                                                String code, EmailCompanyScope scope) {
        EmailNotificationRecipientRule rule = new EmailNotificationRecipientRule();
        rule.setEvent(event);
        rule.setRecipientType(type);
        rule.setRecipientCode(code);
        rule.setCompanyScope(scope);
        rule.setStatus(Status.ACTIVE);
        return rule;
    }

    private CompanyTypes company(String code) {
        CompanyTypes company = new CompanyTypes();
        company.setCode(code);
        company.setStatus(Status.ACTIVE);
        return company;
    }

    private WebUser user(Long id, String username, String email, CompanyTypes company) {
        WebUser user = new WebUser();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setStatus(Status.ACTIVE);
        user.setCompanies(new LinkedHashSet<>(List.of(company)));
        return user;
    }
}

package com.dtech.admin.service;

import com.dtech.admin.enums.*;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.EmailNotificationEventRepository;
import com.dtech.admin.repository.EmailNotificationRecipientRuleRepository;
import com.dtech.admin.repository.WebUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class DependentEmailRecipientServiceTest {
    private EmailNotificationEventRepository eventRepository;
    private EmailNotificationRecipientRuleRepository ruleRepository;
    private WebUserRepository webUserRepository;
    private DependentEmailRecipientService service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EmailNotificationEventRepository.class);
        ruleRepository = mock(EmailNotificationRecipientRuleRepository.class);
        webUserRepository = mock(WebUserRepository.class);
        service = new DependentEmailRecipientService(eventRepository, ruleRepository, webUserRepository);
    }

    @Test
    void approvedEventPreservesSameCompanyAndGlobalAdminRecipients() {
        EmailNotificationEvent event = event(DependentEmailEvent.DEPENDENT_APPROVED, Status.ACTIVE);
        EmailNotificationRecipientRule rule = rule(event, "ADMIN");
        WebUser sameCompany = user(1L, "ADMIN", company("SGCS"));
        WebUser otherCompany = user(2L, "ADMIN", company("OTHER"));
        WebUser global = user(3L, "ADMIN");
        when(eventRepository.findByCode(DependentEmailEvent.DEPENDENT_APPROVED.name())).thenReturn(Optional.of(event));
        when(ruleRepository.findAllByEvent_CodeAndEvent_StatusAndStatus(
                DependentEmailEvent.DEPENDENT_APPROVED.name(), Status.ACTIVE, Status.ACTIVE)).thenReturn(List.of(rule));
        when(webUserRepository.findAllByUserRole_CodeIgnoreCaseAndStatus("ADMIN", Status.ACTIVE))
                .thenReturn(List.of(sameCompany, otherCompany, global));

        assertEquals(List.of(sameCompany, global),
                service.resolve(DependentEmailEvent.DEPENDENT_APPROVED, dependent("SGCS")));
    }

    @Test
    void rejectedEmailEventIsInactiveAndDoesNotAffectExistingSms() {
        EmailNotificationEvent event = event(DependentEmailEvent.DEPENDENT_REJECTED, Status.INACTIVE);
        when(eventRepository.findByCode(DependentEmailEvent.DEPENDENT_REJECTED.name())).thenReturn(Optional.of(event));

        assertTrue(service.resolve(DependentEmailEvent.DEPENDENT_REJECTED, dependent("SGCS")).isEmpty());
        verifyNoInteractions(ruleRepository);
        verifyNoInteractions(webUserRepository);
    }

    private EmailNotificationEvent event(DependentEmailEvent code, Status status) {
        EmailNotificationEvent event = new EmailNotificationEvent();
        event.setCode(code.name());
        event.setCategory("DEPENDENT_MANAGEMENT");
        event.setDescription(code.name());
        event.setStatus(status);
        return event;
    }

    private EmailNotificationRecipientRule rule(EmailNotificationEvent event, String role) {
        EmailNotificationRecipientRule rule = new EmailNotificationRecipientRule();
        rule.setEvent(event);
        rule.setRecipientType(EmailRecipientType.USER_ROLE);
        rule.setRecipientCode(role);
        rule.setCompanyScope(EmailCompanyScope.SAME_COMPANY_OR_UNASSIGNED);
        rule.setStatus(Status.ACTIVE);
        return rule;
    }

    private ClaimsDependents dependent(String companyCode) {
        UserCompanyDetails details = new UserCompanyDetails();
        details.setCompanyTypes(company(companyCode));
        UserPersonalDetails personal = new UserPersonalDetails();
        personal.setUserCompanyDetails(details);
        ApplicationUser employee = new ApplicationUser();
        employee.setUserPersonalDetails(personal);
        ClaimsDependents dependent = new ClaimsDependents();
        dependent.setApplicationUser(employee);
        return dependent;
    }

    private CompanyTypes company(String code) {
        CompanyTypes company = new CompanyTypes();
        company.setCode(code);
        company.setStatus(Status.ACTIVE);
        return company;
    }

    private WebUser user(Long id, String roleCode, CompanyTypes... companies) {
        WebUserRole role = new WebUserRole();
        role.setCode(roleCode);
        WebUser user = new WebUser();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setUserRole(role);
        user.setCompanies(new LinkedHashSet<>(List.of(companies)));
        return user;
    }
}

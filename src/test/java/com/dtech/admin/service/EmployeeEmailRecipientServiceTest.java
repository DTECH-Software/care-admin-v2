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

class EmployeeEmailRecipientServiceTest {
    private EmailNotificationEventRepository eventRepository;
    private EmailNotificationRecipientRuleRepository ruleRepository;
    private WebUserRepository webUserRepository;
    private EmployeeEmailRecipientService service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EmailNotificationEventRepository.class);
        ruleRepository = mock(EmailNotificationRecipientRuleRepository.class);
        webUserRepository = mock(WebUserRepository.class);
        service = new EmployeeEmailRecipientService(eventRepository, ruleRepository, webUserRepository);
    }

    @Test
    void configuredRulePreservesSameCompanyAndUnassignedRecipientBehavior() {
        EmployeeEmailEvent eventCode = EmployeeEmailEvent.EMPLOYEE_INCLUSION;
        EmailNotificationEvent event = event(eventCode, Status.ACTIVE);
        EmailNotificationRecipientRule rule = rule(event, "ADMIN");
        WebUser sameCompany = user(1L, "ADMIN", "same@example.com", company("SGCS"));
        WebUser otherCompany = user(2L, "ADMIN", "other@example.com", company("OTHER"));
        WebUser unassigned = user(3L, "ADMIN", "global@example.com");
        when(eventRepository.findByCode(eventCode.name())).thenReturn(Optional.of(event));
        when(ruleRepository.findAllByEvent_CodeAndEvent_StatusAndStatus(
                eventCode.name(), Status.ACTIVE, Status.ACTIVE)).thenReturn(List.of(rule));
        when(webUserRepository.findAllByUserRole_CodeIgnoreCaseAndStatus("ADMIN", Status.ACTIVE))
                .thenReturn(List.of(sameCompany, otherCompany, unassigned));

        assertEquals(List.of(sameCompany, unassigned), service.resolve(eventCode, employee("SGCS")));
    }

    @Test
    void missingConfigurationUsesExactLegacyRoleAndCompanyRules() {
        EmployeeEmailEvent eventCode = EmployeeEmailEvent.EMPLOYEE_DEACTIVATION;
        WebUser sameCompanyAdmin = user(1L, "ADMIN", "same@example.com", company("SGCS"));
        WebUser otherCompanyAdmin = user(2L, "ADMIN", "other@example.com", company("OTHER"));
        WebUser sameCompanyHr = user(3L, "HRADMIN", "hr@example.com", company("SGCS"));
        WebUser unassignedApprover = user(4L, "APPROVER", "global@example.com");
        when(eventRepository.findByCode(eventCode.name())).thenReturn(Optional.empty());
        when(webUserRepository.findAllByStatus(Status.ACTIVE)).thenReturn(
                List.of(sameCompanyAdmin, otherCompanyAdmin, sameCompanyHr, unassignedApprover));

        assertEquals(List.of(sameCompanyAdmin, unassignedApprover),
                service.resolve(eventCode, employee("SGCS")));
    }

    @Test
    void inactiveEventStopsOnlyThatEmailWithoutLegacyFallback() {
        EmployeeEmailEvent eventCode = EmployeeEmailEvent.STAFF_CATEGORY_TRANSFER_OR_PROMOTION;
        when(eventRepository.findByCode(eventCode.name())).thenReturn(Optional.of(event(eventCode, Status.INACTIVE)));

        assertTrue(service.resolve(eventCode, employee("SGCS")).isEmpty());
        verifyNoInteractions(ruleRepository);
        verifyNoInteractions(webUserRepository);
    }

    private EmailNotificationEvent event(EmployeeEmailEvent code, Status status) {
        EmailNotificationEvent event = new EmailNotificationEvent();
        event.setCode(code.name());
        event.setCategory("EMPLOYEE_MANAGEMENT");
        event.setDescription(code.name());
        event.setStatus(status);
        return event;
    }

    private EmailNotificationRecipientRule rule(EmailNotificationEvent event, String roleCode) {
        EmailNotificationRecipientRule rule = new EmailNotificationRecipientRule();
        rule.setId(1L);
        rule.setEvent(event);
        rule.setRecipientType(EmailRecipientType.USER_ROLE);
        rule.setRecipientCode(roleCode);
        rule.setCompanyScope(EmailCompanyScope.SAME_COMPANY_OR_UNASSIGNED);
        rule.setStatus(Status.ACTIVE);
        return rule;
    }

    private UserPersonalDetails employee(String companyCode) {
        UserCompanyDetails details = new UserCompanyDetails();
        details.setCompanyTypes(company(companyCode));
        UserPersonalDetails employee = new UserPersonalDetails();
        employee.setUserCompanyDetails(details);
        return employee;
    }

    private CompanyTypes company(String code) {
        CompanyTypes company = new CompanyTypes();
        company.setCode(code);
        company.setDescription(code);
        company.setStatus(Status.ACTIVE);
        return company;
    }

    private WebUser user(Long id, String roleCode, String email, CompanyTypes... companies) {
        WebUserRole role = new WebUserRole();
        role.setCode(roleCode);
        WebUser user = new WebUser();
        user.setId(id);
        user.setUserRole(role);
        user.setEmail(email);
        user.setCompanies(new LinkedHashSet<>(List.of(companies)));
        return user;
    }
}

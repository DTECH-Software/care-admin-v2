package com.dtech.admin.service;

import com.dtech.admin.enums.*;
import com.dtech.admin.model.EmailNotificationEvent;
import com.dtech.admin.model.EmailNotificationRecipientRule;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.EmailNotificationEventRepository;
import com.dtech.admin.repository.EmailNotificationRecipientRuleRepository;
import com.dtech.admin.repository.WebUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ClaimEmailRecipientServiceTest {
    private EmailNotificationEventRepository eventRepository;
    private EmailNotificationRecipientRuleRepository ruleRepository;
    private WebUserRepository webUserRepository;
    private ClaimEmailRecipientService service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EmailNotificationEventRepository.class);
        ruleRepository = mock(EmailNotificationRecipientRuleRepository.class);
        webUserRepository = mock(WebUserRepository.class);
        service = new ClaimEmailRecipientService(eventRepository, ruleRepository, webUserRepository);
    }

    @Test
    void resolvesConfiguredApprovalLevelWithoutChangingLegacyRecipientPopulation() {
        ClaimEmailEvent eventCode = ClaimEmailEvent.CLAIM_L1_APPROVED;
        EmailNotificationEvent event = event(eventCode, Status.ACTIVE);
        EmailNotificationRecipientRule rule = rule(event, EmailRecipientType.APPROVAL_LEVEL,
                "LEVEL02", EmailCompanyScope.ALL_COMPANIES);
        WebUser user = user(1L, "level2@example.com");
        when(eventRepository.findByCode(eventCode.name())).thenReturn(Optional.of(event));
        when(ruleRepository.findAllByEvent_CodeAndEvent_StatusAndStatus(
                eventCode.name(), Status.ACTIVE, Status.ACTIVE)).thenReturn(List.of(rule));
        when(webUserRepository.findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL02, Status.ACTIVE))
                .thenReturn(List.of(user));

        assertEquals(List.of(user), service.resolve(eventCode, null, ApprovalLevel.LEVEL02));
        verify(webUserRepository).findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL02, Status.ACTIVE);
    }

    @Test
    void fallsBackToExistingApprovalLevelWhenConfigurationHasNotBeenDeployed() {
        ClaimEmailEvent eventCode = ClaimEmailEvent.CLAIM_L1_REJECTED;
        WebUser user = user(2L, "fallback@example.com");
        when(eventRepository.findByCode(eventCode.name())).thenReturn(Optional.empty());
        when(webUserRepository.findAllByApprovalLevelAndStatus(ApprovalLevel.LEVEL02, Status.ACTIVE))
                .thenReturn(List.of(user));

        assertEquals(List.of(user), service.resolve(eventCode, null, ApprovalLevel.LEVEL02));
    }

    @Test
    void inactiveConfiguredEventStopsOnlyItsEmailAndDoesNotUseFallback() {
        ClaimEmailEvent eventCode = ClaimEmailEvent.CLAIM_L3_FINAL_DECISION;
        when(eventRepository.findByCode(eventCode.name())).thenReturn(Optional.of(event(eventCode, Status.INACTIVE)));

        assertTrue(service.resolve(eventCode, null, ApprovalLevel.LEVEL01).isEmpty());
        verifyNoInteractions(ruleRepository);
        verifyNoInteractions(webUserRepository);
    }

    @Test
    void sameCompanyRuleUsesCompanyScopedUserQuery() {
        ClaimEmailEvent eventCode = ClaimEmailEvent.CLAIM_L2_DIFFERENT_DECISION;
        EmailNotificationEvent event = event(eventCode, Status.ACTIVE);
        EmailNotificationRecipientRule rule = rule(event, EmailRecipientType.USER_ROLE,
                "APPROVER", EmailCompanyScope.SAME_COMPANY);
        when(eventRepository.findByCode(eventCode.name())).thenReturn(Optional.of(event));
        when(ruleRepository.findAllByEvent_CodeAndEvent_StatusAndStatus(
                eventCode.name(), Status.ACTIVE, Status.ACTIVE)).thenReturn(List.of(rule));
        WebUser user = user(3L, "company-approver@example.com");
        when(webUserRepository.findAllByUserRole_CodeIgnoreCaseAndStatusAndCompanies_CodeIgnoreCase(
                "APPROVER", Status.ACTIVE, "SGCS")).thenReturn(List.of(user));

        assertEquals(List.of(user), service.resolve(eventCode, claim("SGCS"), ApprovalLevel.LEVEL03));
        verify(webUserRepository).findAllByUserRole_CodeIgnoreCaseAndStatusAndCompanies_CodeIgnoreCase(
                "APPROVER", Status.ACTIVE, "SGCS");
    }

    private EmailNotificationEvent event(ClaimEmailEvent code, Status status) {
        EmailNotificationEvent event = new EmailNotificationEvent();
        event.setCode(code.name());
        event.setCategory("CLAIM_WORKFLOW");
        event.setDescription(code.name());
        event.setStatus(status);
        return event;
    }

    private EmailNotificationRecipientRule rule(EmailNotificationEvent event,
                                                EmailRecipientType type,
                                                String code,
                                                EmailCompanyScope scope) {
        EmailNotificationRecipientRule rule = new EmailNotificationRecipientRule();
        rule.setId(1L);
        rule.setEvent(event);
        rule.setRecipientType(type);
        rule.setRecipientCode(code);
        rule.setCompanyScope(scope);
        rule.setStatus(Status.ACTIVE);
        return rule;
    }

    private WebUser user(Long id, String email) {
        WebUser user = new WebUser();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private InsuranceClaimsRequest claim(String companyCode) {
        CompanyTypes company = new CompanyTypes();
        company.setCode(companyCode);
        UserCompanyDetails companyDetails = new UserCompanyDetails();
        companyDetails.setCompanyTypes(company);
        UserPersonalDetails personalDetails = new UserPersonalDetails();
        personalDetails.setUserCompanyDetails(companyDetails);
        ApplicationUser employee = new ApplicationUser();
        employee.setUserPersonalDetails(personalDetails);
        InsuranceClaimsRequest claim = new InsuranceClaimsRequest();
        claim.setEmployee(employee);
        return claim;
    }
}

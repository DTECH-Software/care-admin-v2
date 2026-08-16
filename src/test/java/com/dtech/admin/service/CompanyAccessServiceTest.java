package com.dtech.admin.service;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.WebUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyAccessServiceTest {
    private WebUserRepository webUserRepository;
    private CompanyAccessService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        webUserRepository = mock(WebUserRepository.class);
        service = new CompanyAccessService(webUserRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsOnlyActiveCompaniesAssignedToActiveUser() {
        CompanyTypes assignedActive = company("SGCS", "SGCS", Status.ACTIVE);
        CompanyTypes assignedInactive = company("OLD", "Old Company", Status.INACTIVE);
        WebUser user = new WebUser();
        user.setCompanies(new LinkedHashSet<>(java.util.List.of(assignedInactive, assignedActive)));
        when(webUserRepository.findByUsernameAndStatus("hr.user", Status.ACTIVE)).thenReturn(Optional.of(user));

        assertEquals(java.util.List.of(assignedActive), service.activeCompanies("hr.user"));
        assertEquals(java.util.Set.of("SGCS"), service.activeCompanyCodes("hr.user"));
        assertTrue(service.canAccess("hr.user", "sgcs"));
        assertFalse(service.canAccess("hr.user", "OLD"));
    }

    @Test
    void missingInactiveOrUnassignedUserNeverFallsBackToAllCompanies() {
        when(webUserRepository.findByUsernameAndStatus("unknown", Status.ACTIVE)).thenReturn(Optional.empty());

        assertTrue(service.activeCompanies("unknown").isEmpty());
        assertTrue(service.activeCompanyCodes("unknown").isEmpty());
        assertFalse(service.canAccess("unknown", "SGCS"));
        assertFalse(service.canAccess(null, "SGCS"));
    }

    @Test
    void authenticatedJwtIdentityOverridesUsernameFromRequestBody() {
        CompanyTypes assigned = company("SGCS", "SGCS", Status.ACTIVE);
        WebUser authenticatedUser = new WebUser();
        authenticatedUser.setCompanies(new LinkedHashSet<>(java.util.List.of(assigned)));
        when(webUserRepository.findByUsernameAndStatus("authenticated.hr", Status.ACTIVE))
                .thenReturn(Optional.of(authenticatedUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("authenticated.hr", null, java.util.List.of()));

        assertTrue(service.canAccess("spoofed.admin", "SGCS"));
        assertFalse(service.canAccess("spoofed.admin", "OTHER"));
    }

    private CompanyTypes company(String code, String description, Status status) {
        CompanyTypes company = new CompanyTypes();
        company.setCode(code);
        company.setDescription(description);
        company.setStatus(status);
        return company;
    }
}

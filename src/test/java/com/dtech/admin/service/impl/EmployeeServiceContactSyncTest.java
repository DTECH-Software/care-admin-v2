package com.dtech.admin.service.impl;

import com.dtech.admin.mapper.audit.EmployeeDetailsAuditMapper;
import com.dtech.admin.mapper.dtoToEntity.EmployeeDetailsMapperDtoToEntity;
import com.dtech.admin.mapper.entityToDto.EmployeeDetailsMapperEntityToDto;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.*;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EmployeeServiceContactSyncTest {

    private ApplicationUserRepository applicationUserRepository;
    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        applicationUserRepository = mock(ApplicationUserRepository.class);
        service = new EmployeeServiceImpl(
                mock(MessageSource.class),
                new ResponseUtil(),
                mock(AuditLogService.class),
                new Gson(),
                mock(CommonPrivilegeGetter.class),
                mock(DocumentStorageService.class),
                mock(CompanyTypeRepository.class),
                mock(CompanyAccessService.class),
                mock(StaffCategoriesRepository.class),
                mock(StaffTypesRepository.class),
                mock(InsurancePolicyRepository.class),
                mock(UserPersonalDetailsRepository.class),
                applicationUserRepository,
                mock(ClaimDependentsRepository.class),
                mock(EmployeeInactivationGuardService.class),
                mock(InsurancePolicyStaffCategoryReferenceService.class),
                mock(EmployeeDetailsMapperEntityToDto.class),
                mock(EmployeeDetailsMapperDtoToEntity.class),
                mock(EmployeeDetailsAuditMapper.class),
                mock(EmailNotificationService.class),
                mock(EmployeeEmailRecipientService.class));
    }

    @Test
    void employeeEditSynchronizesEmailAndMobileForOnboardedUser() {
        UserPersonalDetails personalDetails = new UserPersonalDetails();
        personalDetails.setEmail("new.email@example.com");
        personalDetails.setMobileNo("0761234567");

        ApplicationUser applicationUser = new ApplicationUser();
        applicationUser.setPrimaryEmail("old.email@example.com");
        applicationUser.setPrimaryMobile("0711234567");
        when(applicationUserRepository.findByUserPersonalDetails(personalDetails))
                .thenReturn(Optional.of(applicationUser));

        service.syncApplicationUserContactDetails(personalDetails);

        assertEquals("new.email@example.com", applicationUser.getPrimaryEmail());
        assertEquals("0761234567", applicationUser.getPrimaryMobile());
        verify(applicationUserRepository).saveAndFlush(applicationUser);
    }

    @Test
    void employeeEditDoesNotCreateApplicationUserBeforeOnboarding() {
        UserPersonalDetails personalDetails = new UserPersonalDetails();
        when(applicationUserRepository.findByUserPersonalDetails(personalDetails))
                .thenReturn(Optional.empty());

        service.syncApplicationUserContactDetails(personalDetails);

        verify(applicationUserRepository, never()).saveAndFlush(any());
    }
}

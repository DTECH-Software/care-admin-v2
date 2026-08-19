package com.dtech.admin.service.impl;

import com.dtech.admin.dto.request.EmployeeDetailsRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.mapper.audit.EmployeeDetailsAuditMapper;
import com.dtech.admin.mapper.dtoToEntity.EmployeeDetailsMapperDtoToEntity;
import com.dtech.admin.mapper.entityToDto.EmployeeDetailsMapperEntityToDto;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.*;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmployeeServiceDeleteGuardTest {

    private MessageSource messageSource;
    private UserPersonalDetailsRepository userPersonalDetailsRepository;
    private ApplicationUserRepository applicationUserRepository;
    private ClaimDependentsRepository claimDependentsRepository;
    private CompanyAccessService companyAccessService;
    private AuditLogService auditLogService;
    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        messageSource = mock(MessageSource.class);
        userPersonalDetailsRepository = mock(UserPersonalDetailsRepository.class);
        applicationUserRepository = mock(ApplicationUserRepository.class);
        claimDependentsRepository = mock(ClaimDependentsRepository.class);
        companyAccessService = mock(CompanyAccessService.class);
        auditLogService = mock(AuditLogService.class);
        service = new EmployeeServiceImpl(
                messageSource,
                new ResponseUtil(),
                auditLogService,
                new Gson(),
                mock(CommonPrivilegeGetter.class),
                mock(DocumentStorageService.class),
                mock(CompanyTypeRepository.class),
                companyAccessService,
                mock(StaffCategoriesRepository.class),
                mock(StaffTypesRepository.class),
                mock(InsurancePolicyRepository.class),
                userPersonalDetailsRepository,
                applicationUserRepository,
                claimDependentsRepository,
                mock(EmployeeInactivationGuardService.class),
                mock(InsurancePolicyStaffCategoryReferenceService.class),
                mock(EmployeeDetailsMapperEntityToDto.class),
                mock(EmployeeDetailsMapperDtoToEntity.class),
                mock(EmployeeDetailsAuditMapper.class),
                mock(EmailNotificationService.class),
                mock(EmployeeEmailRecipientService.class));
    }

    @Test
    void deleteIsBlockedWhenApplicationUserExists() {
        UserPersonalDetails employee = employee();
        ApplicationUser applicationUser = applicationUser(employee);
        arrangeAccessibleEmployee(employee);
        when(applicationUserRepository.findByUserPersonalDetails(employee))
                .thenReturn(Optional.of(applicationUser));
        when(claimDependentsRepository.existsByApplicationUser(applicationUser)).thenReturn(false);
        when(messageSource.getMessage(anyString(), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Care App account exists");

        ResponseEntity<ApiResponse<Object>> response = service.delete(request(), Locale.ENGLISH);

        assertFalse(response.getBody().isSuccess());
        assertEquals(1049, response.getBody().getErrorCode());
        verify(userPersonalDetailsRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void deleteIsBlockedWhenDependentsExist() {
        UserPersonalDetails employee = employee();
        ApplicationUser applicationUser = applicationUser(employee);
        arrangeAccessibleEmployee(employee);
        when(applicationUserRepository.findByUserPersonalDetails(employee))
                .thenReturn(Optional.of(applicationUser));
        when(claimDependentsRepository.existsByApplicationUser(applicationUser)).thenReturn(true);
        when(messageSource.getMessage(anyString(), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Dependents exist");

        ResponseEntity<ApiResponse<Object>> response = service.delete(request(), Locale.ENGLISH);

        assertFalse(response.getBody().isSuccess());
        assertEquals(1049, response.getBody().getErrorCode());
        verify(userPersonalDetailsRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditLogService);
    }

    private void arrangeAccessibleEmployee(UserPersonalDetails employee) {
        when(userPersonalDetailsRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(companyAccessService.canAccess("admin", "SGCS")).thenReturn(true);
    }

    private UserPersonalDetails employee() {
        CompanyTypes company = new CompanyTypes();
        company.setCode("SGCS");
        UserCompanyDetails companyDetails = new UserCompanyDetails();
        companyDetails.setCompanyTypes(company);
        UserPersonalDetails employee = new UserPersonalDetails();
        employee.setId(10L);
        employee.setUserCompanyDetails(companyDetails);
        return employee;
    }

    private ApplicationUser applicationUser(UserPersonalDetails employee) {
        ApplicationUser applicationUser = new ApplicationUser();
        applicationUser.setId(20L);
        applicationUser.setUserPersonalDetails(employee);
        return applicationUser;
    }

    private EmployeeDetailsRequestDTO request() {
        EmployeeDetailsRequestDTO request = new EmployeeDetailsRequestDTO();
        request.setId(10L);
        request.setUsername("admin");
        return request;
    }
}

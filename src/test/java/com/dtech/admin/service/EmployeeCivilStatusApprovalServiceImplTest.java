package com.dtech.admin.service;

import com.dtech.admin.dto.request.CivilStatusApprovalRequestDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.event.CivilStatusApprovedEvent;
import com.dtech.admin.mapper.entityToDto.CivilStatusChangeStatusApprovalEntityToDto;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.ApplicationUserRepository;
import com.dtech.admin.repository.MaritalStatusRepository;
import com.dtech.admin.repository.StaffCategoriesRepository;
import com.dtech.admin.service.impl.EmployeeCivilStatusApprovalServiceImpl;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class EmployeeCivilStatusApprovalServiceImplTest {

    private MaritalStatusRepository maritalStatusRepository;
    private ApplicationUserRepository applicationUserRepository;
    private CompanyAccessService companyAccessService;
    private ApplicationEventPublisher applicationEventPublisher;
    private EmployeeCivilStatusApprovalServiceImpl service;

    @BeforeEach
    void setUp() {
        maritalStatusRepository = mock(MaritalStatusRepository.class);
        applicationUserRepository = mock(ApplicationUserRepository.class);
        companyAccessService = mock(CompanyAccessService.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        service = new EmployeeCivilStatusApprovalServiceImpl(
                mock(CommonPrivilegeGetter.class),
                mock(MessageSource.class),
                new ResponseUtil(),
                mock(AuditLogService.class),
                new Gson(),
                mock(StaffCategoriesRepository.class),
                maritalStatusRepository,
                mock(CivilStatusChangeStatusApprovalEntityToDto.class),
                applicationEventPublisher,
                mock(MessageService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "applicationUserRepository", applicationUserRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "companyAccessService", companyAccessService);
    }

    @Test
    void approvalUpdatesEmployeeStatusAndLinksApprovedRequest() {
        com.dtech.admin.model.MaritalStatus request = request(
                com.dtech.admin.enums.MaritalStatus.DIVORCE,
                com.dtech.admin.enums.MaritalStatus.MARRIED);
        stubRequest(request);

        service.updateStatus(updateRequest("APPROVED"), Locale.ENGLISH);

        UserPersonalDetails personalDetails = request.getApplicationUser().getUserPersonalDetails();
        assertEquals(com.dtech.admin.enums.MaritalStatus.DIVORCE, personalDetails.getMaritalStatus());
        assertEquals(request, personalDetails.getMaritalDetails());
        verify(applicationUserRepository).saveAndFlush(request.getApplicationUser());
        verify(applicationEventPublisher).publishEvent(new CivilStatusApprovedEvent(10L, "hr-user"));
    }

    @Test
    void rejectionDoesNotChangeCurrentEmployeeStatusOrDocumentLink() {
        com.dtech.admin.model.MaritalStatus request = request(
                com.dtech.admin.enums.MaritalStatus.MARRIED,
                com.dtech.admin.enums.MaritalStatus.UNMARRIED);
        stubRequest(request);

        service.updateStatus(updateRequest("REJECTED"), Locale.ENGLISH);

        UserPersonalDetails personalDetails = request.getApplicationUser().getUserPersonalDetails();
        assertEquals(com.dtech.admin.enums.MaritalStatus.UNMARRIED, personalDetails.getMaritalStatus());
        assertNull(personalDetails.getMaritalDetails());
        verify(applicationUserRepository, never()).saveAndFlush(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    private void stubRequest(com.dtech.admin.model.MaritalStatus request) {
        when(maritalStatusRepository.findById(10L)).thenReturn(Optional.of(request));
        when(companyAccessService.canAccess("hr-user", "SGCS")).thenReturn(true);
    }

    private com.dtech.admin.model.MaritalStatus request(
            com.dtech.admin.enums.MaritalStatus requestedStatus,
            com.dtech.admin.enums.MaritalStatus currentStatus) {
        CompanyTypes company = new CompanyTypes();
        company.setCode("SGCS");
        UserCompanyDetails companyDetails = new UserCompanyDetails();
        companyDetails.setCompanyTypes(company);
        UserPersonalDetails personalDetails = new UserPersonalDetails();
        personalDetails.setMaritalStatus(currentStatus);
        personalDetails.setUserCompanyDetails(companyDetails);
        ApplicationUser user = new ApplicationUser();
        user.setPrimaryMobile("0771234567");
        user.setUserPersonalDetails(personalDetails);
        com.dtech.admin.model.MaritalStatus request = new com.dtech.admin.model.MaritalStatus();
        request.setId(10L);
        request.setStatus(Workflow.UNDER_REVIEW);
        request.setMaritalStatus(requestedStatus);
        request.setApplicationUser(user);
        return request;
    }

    private CivilStatusApprovalRequestDTO updateRequest(String status) {
        CivilStatusApprovalRequestDTO dto = new CivilStatusApprovalRequestDTO();
        dto.setId(10L);
        dto.setStatus(status);
        dto.setUsername("hr-user");
        return dto;
    }
}

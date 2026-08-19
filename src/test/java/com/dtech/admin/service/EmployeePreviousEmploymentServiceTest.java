package com.dtech.admin.service;

import com.dtech.admin.dto.request.EmployeePreviousEmploymentRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.dto.response.EmployeePreviousEmploymentResponseDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.mapper.audit.EmployeeDetailsAuditMapper;
import com.dtech.admin.mapper.dtoToEntity.EmployeeDetailsMapperDtoToEntity;
import com.dtech.admin.mapper.entityToDto.EmployeeDetailsMapperEntityToDto;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.impl.EmployeeServiceImpl;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeePreviousEmploymentServiceTest {
    @Mock private MessageSource messageSource;
    @Mock private AuditLogService auditLogService;
    @Mock private CommonPrivilegeGetter commonPrivilegeGetter;
    @Mock private DocumentStorageService documentStorageService;
    @Mock private CompanyTypeRepository companyTypeRepository;
    @Mock private CompanyAccessService companyAccessService;
    @Mock private StaffCategoriesRepository staffCategoriesRepository;
    @Mock private StaffTypesRepository staffTypesRepository;
    @Mock private InsurancePolicyRepository insurancePolicyRepository;
    @Mock private UserPersonalDetailsRepository userPersonalDetailsRepository;
    @Mock private ApplicationUserRepository applicationUserRepository;
    @Mock private ClaimDependentsRepository claimDependentsRepository;
    @Mock private EmployeeInactivationGuardService employeeInactivationGuardService;
    @Mock private InsurancePolicyStaffCategoryReferenceService insurancePolicyStaffCategoryReferenceService;
    @Mock private EmployeeDetailsMapperEntityToDto employeeDetailsMapperEntityToDto;
    @Mock private EmployeeDetailsMapperDtoToEntity employeeDetailsMapperDtoToEntity;
    @Mock private EmployeeDetailsAuditMapper employeeDetailsAuditMapper;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private EmployeeEmailRecipientService employeeEmailRecipientService;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeServiceImpl(messageSource, new ResponseUtil(), auditLogService, new Gson(),
                commonPrivilegeGetter, documentStorageService, companyTypeRepository, companyAccessService,
                staffCategoriesRepository, staffTypesRepository, insurancePolicyRepository,
                userPersonalDetailsRepository, applicationUserRepository, claimDependentsRepository,
                employeeInactivationGuardService, insurancePolicyStaffCategoryReferenceService,
                employeeDetailsMapperEntityToDto,
                employeeDetailsMapperDtoToEntity, employeeDetailsAuditMapper, emailNotificationService,
                employeeEmailRecipientService);
    }

    @Test
    void returnsPairedPreviousCompanyAndEpfAndRemovesDuplicateHistory() {
        AuthorizationTaskResponseDTO privileges = new AuthorizationTaskResponseDTO();
        privileges.setView(true);
        when(commonPrivilegeGetter.getPrivileges("hr.user", WebPage.EMPM.name())).thenReturn(privileges);
        UserPersonalDetails oldSgcs = previousProfile(3L, "OLD-100", "SGCS", "Samson Group");
        UserPersonalDetails duplicateSgcs = previousProfile(2L, "OLD-100", "SGCS", "Samson Group");
        UserPersonalDetails oldDsi = previousProfile(1L, "OLD-200", "DSI", "D. Samson Industries");
        when(userPersonalDetailsRepository.findAllByNicIgnoreCaseAndUserStatusOrderByIdDesc(
                "901234567V", Status.INACTIVE)).thenReturn(List.of(oldSgcs, duplicateSgcs, oldDsi));

        ResponseEntity<ApiResponse<Object>> result = service.previousEmployment(request(), Locale.ENGLISH);

        assertTrue(result.getBody().isSuccess());
        EmployeePreviousEmploymentResponseDTO data =
                (EmployeePreviousEmploymentResponseDTO) result.getBody().getData();
        assertEquals("901234567V", data.getNic());
        assertEquals(2, data.getPreviousEmployment().size());
        assertEquals("SGCS", data.getPreviousEmployment().get(0).getCompanyCode());
        assertEquals("OLD-100", data.getPreviousEmployment().get(0).getEpfNo());
        assertEquals("DSI", data.getPreviousEmployment().get(1).getCompanyCode());
        assertEquals("OLD-200", data.getPreviousEmployment().get(1).getEpfNo());
        verify(auditLogService).log(eq(WebPage.EMPM.name()), eq("VIEW"), anyString(),
                eq("192.168.1.10"), eq("JUnit"), anyString(), isNull(), eq("hr.user"));
    }

    @Test
    void rejectsLookupWithoutEmployeeViewPrivilege() {
        when(commonPrivilegeGetter.getPrivileges("hr.user", WebPage.EMPM.name()))
                .thenReturn(new AuthorizationTaskResponseDTO());

        ResponseEntity<ApiResponse<Object>> result = service.previousEmployment(request(), Locale.ENGLISH);

        assertEquals(1003, result.getBody().getErrorCode());
        verifyNoInteractions(userPersonalDetailsRepository);
    }

    private EmployeePreviousEmploymentRequestDTO request() {
        EmployeePreviousEmploymentRequestDTO request = new EmployeePreviousEmploymentRequestDTO();
        request.setUsername("hr.user");
        request.setIp("192.168.1.10");
        request.setUserAgent("JUnit");
        request.setMessage("VIEW");
        request.setNic(" 901234567V ");
        return request;
    }

    private UserPersonalDetails previousProfile(Long id, String epf, String companyCode, String companyDescription) {
        CompanyTypes company = new CompanyTypes();
        company.setCode(companyCode);
        company.setDescription(companyDescription);
        UserCompanyDetails companyDetails = new UserCompanyDetails();
        companyDetails.setCompanyTypes(company);
        UserPersonalDetails profile = new UserPersonalDetails();
        profile.setId(id);
        profile.setNic("901234567V");
        profile.setEpfNo(epf);
        profile.setUserStatus(Status.INACTIVE);
        profile.setUserCompanyDetails(companyDetails);
        return profile;
    }
}

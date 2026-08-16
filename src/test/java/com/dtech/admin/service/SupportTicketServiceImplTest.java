package com.dtech.admin.service;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.SupportTicketCreateRequestDTO;
import com.dtech.admin.dto.request.SupportTicketStatusUpdateRequestDTO;
import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.dto.response.AuthorizationTaskResponseDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.SupportTicket;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.impl.SupportTicketServiceImpl;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceImplTest {
    @Mock private SupportTicketRepository ticketRepository;
    @Mock private SupportTicketMessageRepository messageRepository;
    @Mock private SupportTicketAttachmentRepository attachmentRepository;
    @Mock private SupportTicketStatusHistoryRepository historyRepository;
    @Mock private WebUserRepository webUserRepository;
    @Mock private DocumentStorageService documentStorageService;
    @Mock private CommonPrivilegeGetter privilegeGetter;
    @Mock private AuditLogService auditLogService;
    @Mock private SupportTicketEmailRecipientService supportTicketEmailRecipientService;
    @Mock private EmailNotificationService emailNotificationService;

    private SupportTicketServiceImpl service;
    private WebUser user;
    private CompanyTypes company;

    @BeforeEach
    void setUp() {
        service = new SupportTicketServiceImpl(ticketRepository, messageRepository, attachmentRepository,
                historyRepository, webUserRepository, documentStorageService, privilegeGetter,
                auditLogService, supportTicketEmailRecipientService, emailNotificationService,
                new ResponseUtil(), new Gson());
        company = new CompanyTypes();
        company.setId(1L);
        company.setCode("SGCS");
        company.setDescription("SGCS");
        company.setStatus(Status.ACTIVE);
        user = new WebUser();
        user.setUsername("admin.user");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setCompanies(new LinkedHashSet<>(List.of(company)));
        when(webUserRepository.findByUsernameAndStatus("admin.user", Status.ACTIVE)).thenReturn(Optional.of(user));
    }

    @Test
    void referenceDataContainsOnlyAssignedCompaniesAndAdminCategories() {
        when(privilegeGetter.getPrivileges("admin.user", WebPage.SUP_ADMIN.name()))
                .thenReturn(privileges(true, true, true, true));
        ChannelRequestDTO request = new ChannelRequestDTO();
        request.setUsername("admin.user");

        ResponseEntity<ApiResponse<Object>> response = service.referenceData(
                request, SupportTicketSystemType.WECARE_ADMIN, Locale.ENGLISH);

        assertTrue(response.getBody().isSuccess());
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        @SuppressWarnings("unchecked") List<SimpleBaseDTO> companies = (List<SimpleBaseDTO>) data.get("companies");
        @SuppressWarnings("unchecked") List<SimpleBaseDTO> categories = (List<SimpleBaseDTO>) data.get("categories");
        assertEquals(List.of("SGCS"), companies.stream().map(SimpleBaseDTO::getCode).toList());
        assertTrue(categories.stream().anyMatch(value -> "CLAIM_APPROVAL".equals(value.getCode())));
        assertFalse(categories.stream().anyMatch(value -> "LOGIN_OTP".equals(value.getCode())));
    }

    @Test
    void createRejectsACompanyNotAssignedToTheUser() {
        when(privilegeGetter.getPrivileges("admin.user", WebPage.SUP_ADMIN.name()))
                .thenReturn(privileges(true, true, true, true));
        SupportTicketCreateRequestDTO request = createRequest();
        request.setCompanyCode("OTHER_COMPANY");

        ResponseEntity<ApiResponse<Object>> response = service.create(
                request, SupportTicketSystemType.WECARE_ADMIN, Locale.ENGLISH);

        assertFalse(response.getBody().isSuccess());
        assertEquals(1044, response.getBody().getErrorCode());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void createUsesThePageToFixSystemTypeAndStartsOpen() {
        when(privilegeGetter.getPrivileges("admin.user", WebPage.SUP_APP.name()))
                .thenReturn(privileges(true, true, true, true));
        when(ticketRepository.saveAndFlush(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket ticket = invocation.getArgument(0);
            ticket.setId(9L);
            ticket.setCreatedBy("admin.user");
            ticket.setCreatedDate(new Date());
            ticket.setLastModifiedDate(new Date());
            return ticket;
        });

        ResponseEntity<ApiResponse<Object>> response = service.create(
                createRequest(), SupportTicketSystemType.WECARE_APP, Locale.ENGLISH);

        assertTrue(response.getBody().isSuccess());
        verify(ticketRepository).saveAndFlush(argThat(ticket ->
                ticket.getSystemType() == SupportTicketSystemType.WECARE_APP
                        && ticket.getStatus() == SupportTicketStatus.OPEN
                        && "SGCS".equals(ticket.getCompany().getCode())));
        verify(auditLogService).log(eq(WebPage.SUP_APP.name()), eq(WebTask.ADD.name()),
                anyString(), anyString(), anyString(), anyString(), isNull(), eq("admin.user"));
        verify(supportTicketEmailRecipientService).resolve(
                eq(SupportTicketEmailEvent.SUPPORT_TICKET_CREATED), any(SupportTicket.class), eq("admin.user"));
    }

    @Test
    void invalidStatusTransitionIsRejected() {
        AuthorizationTaskResponseDTO privileges = privileges(false, true, true, true);
        privileges.setUpdate(true);
        when(privilegeGetter.getPrivileges("admin.user", WebPage.SUP_ADMIN.name())).thenReturn(privileges);
        SupportTicket ticket = new SupportTicket();
        ticket.setId(1L);
        ticket.setSystemType(SupportTicketSystemType.WECARE_ADMIN);
        ticket.setCompany(company);
        ticket.setStatus(SupportTicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        SupportTicketStatusUpdateRequestDTO request = new SupportTicketStatusUpdateRequestDTO();
        request.setUsername("admin.user");
        request.setId(1L);
        request.setStatus("WAITING_FOR_CLIENT");

        ResponseEntity<ApiResponse<Object>> response = service.updateStatus(
                request, SupportTicketSystemType.WECARE_ADMIN, Locale.ENGLISH);

        assertFalse(response.getBody().isSuccess());
        assertEquals(1045, response.getBody().getErrorCode());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    private SupportTicketCreateRequestDTO createRequest() {
        SupportTicketCreateRequestDTO request = new SupportTicketCreateRequestDTO();
        request.setUsername("admin.user");
        request.setIp("192.168.1.10");
        request.setUserAgent("JUnit");
        request.setMessage("ADD");
        request.setCompanyCode("SGCS");
        request.setCategory("OTHER");
        request.setSubject("Test ticket");
        request.setDescription("Test description");
        request.setPriority("MEDIUM");
        return request;
    }

    private AuthorizationTaskResponseDTO privileges(boolean add, boolean search, boolean view, boolean fileUpload) {
        AuthorizationTaskResponseDTO value = new AuthorizationTaskResponseDTO();
        value.setAdd(add);
        value.setSearch(search);
        value.setView(view);
        value.setFileUpload(fileUpload);
        return value;
    }
}

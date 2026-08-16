package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.*;
import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.admin.dto.response.*;
import com.dtech.admin.dto.search.SupportTicketSearchDTO;
import com.dtech.admin.enums.*;
import com.dtech.admin.enums.WebPage;
import com.dtech.admin.enums.WebTask;
import com.dtech.admin.model.*;
import com.dtech.admin.repository.*;
import com.dtech.admin.service.AuditLogService;
import com.dtech.admin.service.DocumentStorageService;
import com.dtech.admin.service.SupportTicketService;
import com.dtech.admin.specifications.SupportTicketSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {
    private static final long MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/jpg");
    private static final Map<SupportTicketSystemType, List<SimpleBaseDTO>> CATEGORIES = Map.of(
            SupportTicketSystemType.WECARE_ADMIN, List.of(
                    category("LOGIN_ACCESS", "Login and access"),
                    category("EMPLOYEE_DEPENDENT", "Employee and dependent"),
                    category("CLAIM_APPROVAL", "Claim approval"),
                    category("REPORT", "Report"),
                    category("USER_PRIVILEGE", "User and privilege"),
                    category("DOCUMENT", "Document"),
                    category("OTHER", "Other")),
            SupportTicketSystemType.WECARE_APP, List.of(
                    category("LOGIN_OTP", "Login and OTP"),
                    category("PROFILE_DEPENDENT", "Profile and dependent"),
                    category("CLAIM_SUBMISSION", "Claim submission"),
                    category("CLAIM_STATUS", "Claim status"),
                    category("DOCUMENT", "Document"),
                    category("NOTIFICATION", "Notification"),
                    category("OTHER", "Other")));
    private static final Map<SupportTicketStatus, Set<SupportTicketStatus>> STATUS_TRANSITIONS = Map.of(
            SupportTicketStatus.OPEN, EnumSet.of(SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.RESOLVED, SupportTicketStatus.CLOSED),
            SupportTicketStatus.IN_PROGRESS, EnumSet.of(SupportTicketStatus.WAITING_FOR_CLIENT, SupportTicketStatus.RESOLVED, SupportTicketStatus.CLOSED),
            SupportTicketStatus.WAITING_FOR_CLIENT, EnumSet.of(SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.RESOLVED, SupportTicketStatus.CLOSED),
            SupportTicketStatus.RESOLVED, EnumSet.of(SupportTicketStatus.REOPENED, SupportTicketStatus.CLOSED),
            SupportTicketStatus.CLOSED, EnumSet.of(SupportTicketStatus.REOPENED),
            SupportTicketStatus.REOPENED, EnumSet.of(SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.WAITING_FOR_CLIENT,
                    SupportTicketStatus.RESOLVED, SupportTicketStatus.CLOSED));

    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final SupportTicketAttachmentRepository attachmentRepository;
    private final SupportTicketStatusHistoryRepository statusHistoryRepository;
    private final WebUserRepository webUserRepository;
    private final DocumentStorageService documentStorageService;
    private final CommonPrivilegeGetter commonPrivilegeGetter;
    private final AuditLogService auditLogService;
    private final ResponseUtil responseUtil;
    private final Gson gson;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> referenceData(ChannelRequestDTO request,
                                                             SupportTicketSystemType systemType,
                                                             Locale locale) {
        Optional<WebUser> optionalUser = activeUser(request.getUsername());
        if (optionalUser.isEmpty()) return userNotFound();
        AuthorizationTaskResponseDTO privileges = privileges(request.getUsername(), systemType);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("privileges", privileges == null ? new AuthorizationTaskResponseDTO() : privileges);
        data.put("companies", activeCompanies(optionalUser.get()).stream()
                .map(company -> new SimpleBaseDTO(company.getCode(), company.getDescription())).toList());
        data.put("categories", CATEGORIES.get(systemType));
        data.put("priorities", Arrays.stream(SupportTicketPriority.values())
                .map(value -> new SimpleBaseDTO(value.name(), value.getDescription())).toList());
        data.put("statuses", Arrays.stream(SupportTicketStatus.values())
                .map(value -> new SimpleBaseDTO(value.name(), value.getDescription())).toList());
        return ResponseEntity.ok(responseUtil.success(data, systemType.getDescription() + " support ticket reference data retrieved successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<SupportTicketSearchDTO> request,
                                                      SupportTicketSystemType systemType,
                                                      Locale locale) {
        if (!hasPrivilege(request.getUsername(), systemType, AuthorizationTaskResponseDTO::isSearch)) return unauthorized();
        Optional<WebUser> optionalUser = activeUser(request.getUsername());
        if (optionalUser.isEmpty()) return userNotFound();
        Set<String> companyCodes = companyCodes(optionalUser.get());
        normalizeSort(request);
        Page<SupportTicket> page = supportTicketRepository.findAll(
                SupportTicketSpecification.filter(systemType, companyCodes, request.getSearch()),
                PaginationUtil.getPageable(request));
        List<SupportTicketResponseDTO> rows = page.stream().map(this::mapSummary).toList();
        audit(request, systemType, WebTask.SEARCH, "Search support tickets",
                Map.of("filters", request.getSearch() == null ? Map.of() : request.getSearch()));
        return ResponseEntity.ok(responseUtil.success(
                new PagingResult<>(rows, rows.size(), page.getTotalElements()), "Support tickets retrieved successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> create(SupportTicketCreateRequestDTO request,
                                                      SupportTicketSystemType systemType,
                                                      Locale locale) {
        if (!hasPrivilege(request.getUsername(), systemType, AuthorizationTaskResponseDTO::isAdd)) return unauthorized();
        Optional<WebUser> optionalUser = activeUser(request.getUsername());
        if (optionalUser.isEmpty()) return userNotFound();
        if (hasAttachments(request.getAttachments())
                && !hasPrivilege(request.getUsername(), systemType, AuthorizationTaskResponseDTO::isFileUpload)) {
            return error(1003, "You are not authorized to upload support ticket attachments");
        }
        CompanyTypes company = assignedCompany(optionalUser.get(), request.getCompanyCode()).orElse(null);
        if (company == null) return error(1044, "The selected company is not assigned to this user");
        String category = normalizeCategory(request.getCategory(), systemType);
        if (category == null) return error(1002, "Invalid support ticket category");
        SupportTicketPriority priority = parseEnum(SupportTicketPriority.class, request.getPriority());
        if (priority == null) return error(1002, "Invalid support ticket priority");
        String attachmentError = validateAttachments(request.getAttachments());
        if (attachmentError != null) return error(1002, attachmentError);

        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo(generateTicketNo(systemType));
        ticket.setSystemType(systemType);
        ticket.setCompany(company);
        ticket.setCategory(category);
        ticket.setSubject(request.getSubject().trim());
        ticket.setDescription(request.getDescription().trim());
        ticket.setPriority(priority);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket = supportTicketRepository.saveAndFlush(ticket);
        addStatusHistory(ticket, null, SupportTicketStatus.OPEN, "Ticket created");
        saveAttachments(ticket, null, request.getAttachments());
        audit(request, systemType, WebTask.ADD, "Create support ticket", auditTicket(ticket));
        return ResponseEntity.ok(responseUtil.success(mapSummary(ticket),
                "Support ticket " + ticket.getTicketNo() + " created successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> view(SupportTicketViewRequestDTO request,
                                                    SupportTicketSystemType systemType,
                                                    Locale locale) {
        if (!hasPrivilege(request.getUsername(), systemType, AuthorizationTaskResponseDTO::isView)) return unauthorized();
        Optional<SupportTicket> ticket = accessibleTicket(request.getId(), request.getUsername(), systemType);
        if (ticket.isEmpty()) return ticketNotFound();
        audit(request, systemType, WebTask.VIEW, "View support ticket", Map.of("id", ticket.get().getId()));
        return ResponseEntity.ok(responseUtil.success(mapDetails(ticket.get()), "Support ticket retrieved successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> reply(SupportTicketReplyRequestDTO request,
                                                     SupportTicketSystemType systemType,
                                                     Locale locale) {
        if (!hasPrivilege(request.getUsername(), systemType, AuthorizationTaskResponseDTO::isAdd)) return unauthorized();
        if (hasAttachments(request.getAttachments())
                && !hasPrivilege(request.getUsername(), systemType, AuthorizationTaskResponseDTO::isFileUpload)) {
            return error(1003, "You are not authorized to upload support ticket attachments");
        }
        Optional<SupportTicket> optionalTicket = accessibleTicket(request.getId(), request.getUsername(), systemType);
        if (optionalTicket.isEmpty()) return ticketNotFound();
        SupportTicket ticket = optionalTicket.get();
        if (ticket.getStatus() == SupportTicketStatus.CLOSED)
            return error(1045, "A closed support ticket must be reopened before adding a reply");
        String attachmentError = validateAttachments(request.getAttachments());
        if (attachmentError != null) return error(1002, attachmentError);
        WebUser user = activeUser(request.getUsername()).orElseThrow();

        SupportTicketMessage message = new SupportTicketMessage();
        message.setTicket(ticket);
        message.setAuthorUsername(user.getUsername());
        message.setAuthorName(displayName(user));
        message.setMessage(request.getReply().trim());
        message = messageRepository.saveAndFlush(message);
        ticket.getMessages().add(message);
        saveAttachments(ticket, message, request.getAttachments());
        ticket.setLastModifiedDate(new Date());
        supportTicketRepository.saveAndFlush(ticket);
        audit(request, systemType, WebTask.ADD, "Reply to support ticket",
                Map.of("id", ticket.getId(), "ticketNo", ticket.getTicketNo(), "messageId", message.getId()));
        return ResponseEntity.ok(responseUtil.success(mapDetails(ticket), "Support ticket reply added successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> updateStatus(SupportTicketStatusUpdateRequestDTO request,
                                                            SupportTicketSystemType systemType,
                                                            Locale locale) {
        if (!hasPrivilege(request.getUsername(), systemType, AuthorizationTaskResponseDTO::isUpdate)) return unauthorized();
        Optional<SupportTicket> optionalTicket = accessibleTicket(request.getId(), request.getUsername(), systemType);
        if (optionalTicket.isEmpty()) return ticketNotFound();
        SupportTicketStatus newStatus = parseEnum(SupportTicketStatus.class, request.getStatus());
        if (newStatus == null) return error(1002, "Invalid support ticket status");
        SupportTicket ticket = optionalTicket.get();
        SupportTicketStatus oldStatus = ticket.getStatus();
        if (oldStatus == newStatus) return error(1045, "Support ticket is already in " + newStatus.getDescription() + " status");
        if (!STATUS_TRANSITIONS.getOrDefault(oldStatus, Set.of()).contains(newStatus))
            return error(1045, "Status cannot be changed from " + oldStatus.getDescription() + " to " + newStatus.getDescription());
        if ((newStatus == SupportTicketStatus.RESOLVED || newStatus == SupportTicketStatus.CLOSED)
                && !StringUtils.hasText(request.getResolution()) && !StringUtils.hasText(ticket.getResolution())) {
            return error(1002, "Resolution is required when resolving or closing a support ticket");
        }

        Date now = new Date();
        ticket.setStatus(newStatus);
        if (StringUtils.hasText(request.getResolution())) ticket.setResolution(request.getResolution().trim());
        if (newStatus == SupportTicketStatus.RESOLVED) ticket.setResolvedDate(now);
        if (newStatus == SupportTicketStatus.CLOSED) ticket.setClosedDate(now);
        if (newStatus == SupportTicketStatus.REOPENED) {
            ticket.setResolvedDate(null);
            ticket.setClosedDate(null);
            ticket.setResolution(null);
        }
        ticket = supportTicketRepository.saveAndFlush(ticket);
        addStatusHistory(ticket, oldStatus, newStatus, request.getRemark());
        audit(request, systemType, WebTask.UPDATE, "Update support ticket status",
                Map.of("id", ticket.getId(), "ticketNo", ticket.getTicketNo(),
                        "oldStatus", oldStatus.name(), "newStatus", newStatus.name()));
        return ResponseEntity.ok(responseUtil.success(mapDetails(ticket), "Support ticket status updated successfully"));
    }

    private SupportTicketResponseDTO mapSummary(SupportTicket ticket) {
        return baseResponse(ticket).replyCount(ticket.getMessages().size())
                .attachmentCount(ticket.getAttachments().size()).build();
    }

    private SupportTicketResponseDTO mapDetails(SupportTicket ticket) {
        List<SupportTicketAttachmentResponseDTO> ticketAttachments = ticket.getAttachments().stream()
                .filter(value -> value.getMessage() == null).map(this::mapAttachment).toList();
        List<SupportTicketMessageResponseDTO> replies = ticket.getMessages().stream().map(message ->
                SupportTicketMessageResponseDTO.builder()
                        .id(message.getId()).authorUsername(message.getAuthorUsername()).authorName(message.getAuthorName())
                        .message(message.getMessage()).createdDate(message.getCreatedDate())
                        .attachments(ticket.getAttachments().stream()
                                .filter(value -> value.getMessage() != null && Objects.equals(value.getMessage().getId(), message.getId()))
                                .map(this::mapAttachment).toList()).build()).toList();
        List<SupportTicketStatusHistoryResponseDTO> history = ticket.getStatusHistory().stream().map(value ->
                SupportTicketStatusHistoryResponseDTO.builder()
                        .oldStatus(value.getOldStatus() == null ? null : value.getOldStatus().name())
                        .newStatus(value.getNewStatus().name()).remark(value.getRemark())
                        .changedBy(value.getCreatedBy()).changedDate(value.getCreatedDate()).build()).toList();
        return baseResponse(ticket).replyCount(replies.size()).attachmentCount(ticket.getAttachments().size())
                .replies(replies).attachments(ticketAttachments).statusHistory(history).build();
    }

    private SupportTicketResponseDTO.SupportTicketResponseDTOBuilder baseResponse(SupportTicket ticket) {
        return SupportTicketResponseDTO.builder()
                .id(ticket.getId()).ticketNo(ticket.getTicketNo()).systemType(ticket.getSystemType().name())
                .systemDescription(ticket.getSystemType().getDescription())
                .companyCode(ticket.getCompany().getCode()).companyDescription(ticket.getCompany().getDescription())
                .category(ticket.getCategory()).subject(ticket.getSubject()).description(ticket.getDescription())
                .priority(ticket.getPriority().name()).status(ticket.getStatus().name()).resolution(ticket.getResolution())
                .createdBy(ticket.getCreatedBy()).createdDate(ticket.getCreatedDate()).lastModifiedDate(ticket.getLastModifiedDate())
                .resolvedDate(ticket.getResolvedDate()).closedDate(ticket.getClosedDate());
    }

    private SupportTicketAttachmentResponseDTO mapAttachment(SupportTicketAttachment attachment) {
        Document document = attachment.getDocument();
        return SupportTicketAttachmentResponseDTO.builder().id(attachment.getId())
                .messageId(attachment.getMessage() == null ? null : attachment.getMessage().getId())
                .documentId(document.getId()).fileName(document.getFileName()).fileType(document.getFileType())
                .file(documentStorageService.getBase64(document)).build();
    }

    private void saveAttachments(SupportTicket ticket, SupportTicketMessage message,
                                 List<SupportTicketAttachmentRequestDTO> attachments) {
        if (!hasAttachments(attachments)) return;
        for (SupportTicketAttachmentRequestDTO value : attachments) {
            Document document = new Document();
            // The existing document.type column is a restricted database enum.
            // Support-ticket files are ordinary documents, so reuse the compatible value.
            document.setType(DocType.DOCUMENT);
            document.setFileName(safeFileName(value.getFileName()));
            document.setFileType(value.getFileType().trim().toLowerCase());
            document = documentStorageService.saveAdminDocument(document, value.getFile());
            SupportTicketAttachment attachment = new SupportTicketAttachment();
            attachment.setTicket(ticket);
            attachment.setMessage(message);
            attachment.setDocument(document);
            attachment = attachmentRepository.saveAndFlush(attachment);
            ticket.getAttachments().add(attachment);
        }
    }

    private void addStatusHistory(SupportTicket ticket, SupportTicketStatus oldStatus,
                                  SupportTicketStatus newStatus, String remark) {
        SupportTicketStatusHistory history = new SupportTicketStatusHistory();
        history.setTicket(ticket);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        history = statusHistoryRepository.saveAndFlush(history);
        ticket.getStatusHistory().add(history);
    }

    private Optional<SupportTicket> accessibleTicket(Long id, String username, SupportTicketSystemType systemType) {
        Optional<WebUser> user = activeUser(username);
        if (user.isEmpty()) return Optional.empty();
        Set<String> codes = companyCodes(user.get());
        return supportTicketRepository.findById(id).filter(ticket -> ticket.getSystemType() == systemType
                && codes.contains(ticket.getCompany().getCode()));
    }

    private Optional<WebUser> activeUser(String username) {
        if (!StringUtils.hasText(username)) return Optional.empty();
        return webUserRepository.findByUsernameAndStatus(username.trim(), Status.ACTIVE);
    }

    private List<CompanyTypes> activeCompanies(WebUser user) {
        return user.getCompanies().stream().filter(company -> company.getStatus() == Status.ACTIVE)
                .sorted(Comparator.comparing(CompanyTypes::getDescription, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private Set<String> companyCodes(WebUser user) {
        Set<String> codes = new HashSet<>();
        activeCompanies(user).forEach(company -> codes.add(company.getCode()));
        return codes;
    }

    private Optional<CompanyTypes> assignedCompany(WebUser user, String code) {
        if (!StringUtils.hasText(code)) return Optional.empty();
        return activeCompanies(user).stream().filter(company -> company.getCode().equalsIgnoreCase(code.trim())).findFirst();
    }

    private AuthorizationTaskResponseDTO privileges(String username, SupportTicketSystemType systemType) {
        return commonPrivilegeGetter.getPrivileges(username, page(systemType).name());
    }

    private boolean hasPrivilege(String username, SupportTicketSystemType systemType,
                                 Predicate<AuthorizationTaskResponseDTO> predicate) {
        AuthorizationTaskResponseDTO value = privileges(username, systemType);
        return value != null && predicate.test(value);
    }

    private WebPage page(SupportTicketSystemType systemType) {
        return systemType == SupportTicketSystemType.WECARE_ADMIN ? WebPage.SUP_ADMIN : WebPage.SUP_APP;
    }

    private String normalizeCategory(String category, SupportTicketSystemType systemType) {
        if (!StringUtils.hasText(category)) return null;
        String normalized = category.trim().toUpperCase();
        return CATEGORIES.get(systemType).stream().anyMatch(value -> value.getCode().equals(normalized)) ? normalized : null;
    }

    private String validateAttachments(List<SupportTicketAttachmentRequestDTO> attachments) {
        if (!hasAttachments(attachments)) return null;
        for (SupportTicketAttachmentRequestDTO value : attachments) {
            if (!ALLOWED_FILE_TYPES.contains(value.getFileType().trim().toLowerCase()))
                return "Only PNG, JPEG, JPG, and PDF attachments are allowed";
            try {
                String content = value.getFile().trim();
                int separator = content.indexOf(',');
                if (content.startsWith("data:") && separator >= 0) content = content.substring(separator + 1);
                if (Base64.getDecoder().decode(content).length > MAX_ATTACHMENT_BYTES)
                    return "Each support ticket attachment must be 10 MB or smaller";
            } catch (IllegalArgumentException exception) {
                return "Support ticket attachment content must be valid Base64";
            }
        }
        return null;
    }

    private boolean hasAttachments(List<SupportTicketAttachmentRequestDTO> attachments) {
        return attachments != null && !attachments.isEmpty();
    }

    private String safeFileName(String fileName) {
        String normalized = fileName.trim().replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private String generateTicketNo(SupportTicketSystemType systemType) {
        String prefix = systemType == SupportTicketSystemType.WECARE_ADMIN ? "ADM" : "APP";
        return prefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String displayName(WebUser user) {
        return (StringUtils.hasText(user.getFirstName()) ? user.getFirstName().trim() : "")
                + (StringUtils.hasText(user.getLastName()) ? " " + user.getLastName().trim() : "");
    }

    private void normalizeSort(PaginationRequest<SupportTicketSearchDTO> request) {
        String sort = request.getSortColumn();
        if ("company".equals(sort)) request.setSortColumn("company.description");
        else if (!List.of("id", "ticketNo", "category", "subject", "priority", "status", "createdBy",
                "createdDate", "lastModifiedDate", "company.description").contains(sort)) request.setSortColumn("lastModifiedDate");
    }

    private Map<String, Object> auditTicket(SupportTicket ticket) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", ticket.getId());
        value.put("ticketNo", ticket.getTicketNo());
        value.put("systemType", ticket.getSystemType().name());
        value.put("companyCode", ticket.getCompany().getCode());
        value.put("category", ticket.getCategory());
        value.put("priority", ticket.getPriority().name());
        value.put("status", ticket.getStatus().name());
        return value;
    }

    private void audit(ChannelRequestValidatorDTO request, SupportTicketSystemType systemType, WebTask task,
                       String description, Object value) {
        auditLogService.log(page(systemType).name(), task.name(), description, request.getIp(),
                request.getUserAgent(), gson.toJson(value), null, request.getUsername());
    }

    private ResponseEntity<ApiResponse<Object>> unauthorized() {
        return error(1003, "You are not authorized to perform this support ticket action");
    }

    private ResponseEntity<ApiResponse<Object>> userNotFound() {
        return error(1043, "Active Admin user not found");
    }

    private ResponseEntity<ApiResponse<Object>> ticketNotFound() {
        return error(1043, "Support ticket not found or is outside your assigned companies");
    }

    private ResponseEntity<ApiResponse<Object>> error(int code, String message) {
        return ResponseEntity.ok(responseUtil.error(null, code, message));
    }

    private static SimpleBaseDTO category(String code, String description) {
        return new SimpleBaseDTO(code, description);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}

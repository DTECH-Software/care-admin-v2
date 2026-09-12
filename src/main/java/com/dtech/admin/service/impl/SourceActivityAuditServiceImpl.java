package com.dtech.admin.service.impl;

import com.dtech.admin.dto.PagingResult;
import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.request.ChannelRequestDTO;
import com.dtech.admin.dto.request.PaginationRequest;
import com.dtech.admin.dto.response.*;
import com.dtech.admin.dto.search.AuditLogSearchDTO;
import com.dtech.admin.model.AuditLog;
import com.dtech.admin.repository.AuditLogRepository;
import com.dtech.admin.repository.WebPageRepository;
import com.dtech.admin.repository.WebTaskRepository;
import com.dtech.admin.service.SourceActivityAuditService;
import com.dtech.admin.specifications.AuditLogSpecification;
import com.dtech.admin.util.CommonPrivilegeGetter;
import com.dtech.admin.util.PaginationUtil;
import com.dtech.admin.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SourceActivityAuditServiceImpl implements SourceActivityAuditService {
    public static final String ADMIN_SOURCE = "WECARE_ADMIN";
    public static final String APP_SOURCE = "WECARE_APP";

    private final AuditLogRepository auditLogRepository;
    private final WebPageRepository webPageRepository;
    private final WebTaskRepository webTaskRepository;
    private final CommonPrivilegeGetter commonPrivilegeGetter;
    private final ResponseUtil responseUtil;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> getReferenceData(ChannelRequestDTO request, String source,
                                                                String pageCode, Locale locale) {
        AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.getPrivileges(request.getUsername(), pageCode);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("privileges", new AuditLogPrivilegeResponseDTO(
                privileges != null && privileges.isSearch(), privileges != null && privileges.isView()));
        data.put("results", List.of(new SimpleBaseDTO("SUCCESS", "Success"),
                new SimpleBaseDTO("FAILED", "Failed")));

        if (ADMIN_SOURCE.equals(source)) {
            data.put("pages", webPageRepository.findAll().stream()
                    .map(page -> new SimpleBaseDTO(page.getCode(), page.getDescription())).toList());
            data.put("tasks", webTaskRepository.findAll().stream()
                    .filter(task -> !"API_REQUEST".equals(task.getCode()))
                    .map(task -> new SimpleBaseDTO(task.getCode(), task.getDescription())).toList());
        } else {
            data.put("modules", appModules());
            data.put("clientPlatforms", List.of(new SimpleBaseDTO("ANDROID", "Android"),
                    new SimpleBaseDTO("IOS", "iOS")));
            data.put("appUpdateStatuses", List.of(new SimpleBaseDTO("NONE", "No update"),
                    new SimpleBaseDTO("OPTIONAL", "Optional update"),
                    new SimpleBaseDTO("REQUIRED", "Required update"),
                    new SimpleBaseDTO("UNKNOWN", "Unknown")));
        }
        return ResponseEntity.ok(responseUtil.success(data, "Activity reference data retrieved successfully"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> filter(PaginationRequest<AuditLogSearchDTO> request, String source,
                                                      String pageCode, Locale locale) {
        AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.getPrivileges(request.getUsername(), pageCode);
        if (privileges == null || !privileges.isSearch()) return unauthorized();

        AuditLogSearchDTO search = request.getSearch() == null ? new AuditLogSearchDTO() : request.getSearch();
        search.setSource(source); // The page source cannot be overridden by a client payload.
        if (APP_SOURCE.equals(source)) {
            search.setPageCode(null);
            search.setTaskCode(null);
        }
        request.setSearch(search);
        normalizeSort(request);

        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecification.getSpecification(search), PaginationUtil.getPageable(request));
        List<ActivityAuditResponseDTO> rows = page.stream().map(this::mapFriendly).toList();
        return ResponseEntity.ok(responseUtil.success(
                new PagingResult<>(rows, rows.size(), page.getTotalElements()),
                "Activities retrieved successfully"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> view(Long id, String username, String source,
                                                    String pageCode, Locale locale) {
        AuthorizationTaskResponseDTO privileges = commonPrivilegeGetter.getPrivileges(username, pageCode);
        if (privileges == null || !privileges.isView()) return unauthorized();

        return auditLogRepository.findById(id)
                .filter(log -> source.equals(log.getSource()))
                .map(log -> ResponseEntity.ok(responseUtil.success((Object) mapFriendly(log),
                        "Activity retrieved successfully")))
                .orElseGet(() -> ResponseEntity.ok(responseUtil.error(null, 1043, "Activity not found")));
    }

    private ActivityAuditResponseDTO mapFriendly(AuditLog log) {
        String source = log.getSource();
        return ActivityAuditResponseDTO.builder()
                .id(log.getId()).dateTime(log.getCreatedDate())
                .activity(ADMIN_SOURCE.equals(source) ? adminActivity(log) : appActivity(log))
                .module(log.getModule()).moduleDescription(moduleDescription(log))
                .performedBy(emptyAsSystem(log.getCreatedBy())).result(resolveResult(log))
                .ipAddress(log.getIpAddress()).device(log.getUserAgent())
                .correlationId(log.getCorrelationId())
                .clientAppVersion(log.getClientAppVersion()).clientPlatform(log.getClientPlatform())
                .appUpdateStatus(log.getAppUpdateStatus()).build();
    }

    private String adminActivity(AuditLog log) {
        String page = log.getPage() == null ? "WeCare Admin" : log.getPage().getDescription();
        String task = log.getTask() == null ? log.getAction() : log.getTask().getCode();
        if (task == null) return "Performed an activity in " + page;
        return switch (task) {
            case "LOGIN" -> "Signed in to WeCare Admin";
            case "REF_DATA" -> "Opened " + page;
            case "SEARCH", "FILTER_LIST" -> "Searched " + page;
            case "VIEW" -> "Viewed details in " + page;
            case "ADD" -> "Created a record in " + page;
            case "UPDATE" -> "Updated a record in " + page;
            case "DELETE" -> "Deleted a record from " + page;
            case "DEACTIVATE" -> "Deactivated a record in " + page;
            case "PASSWORD_RESET" -> "Reset a user password";
            case "FILE_UPLOAD" -> "Uploaded a file in " + page;
            case "FILE_DOWNLOAD" -> "Downloaded a file from " + page;
            case "STAFF_CAT_UPDATE" -> "Updated an employee staff category";
            case "STAFF_CAT_TRANSFER" -> "Transferred an employee staff category";
            default -> readable(task) + " in " + page;
        };
    }

    private String appActivity(AuditLog log) {
        String path = log.getRequestPath() == null ? "" : log.getRequestPath().toLowerCase();
        if (path.contains("/login/login")) return "Signed in to WeCare App";
        if (path.contains("/login/logout")) return "Signed out from WeCare App";
        if (path.contains("/password/")) return "Managed the WeCare App password";
        if (path.contains("/biometric") || path.contains("enablebiometric")) return "Managed biometric login";
        if (path.contains("/profile/update-marital-status")) return "Requested a marital status update";
        if (path.contains("/profile/update-profile/edit-image")) return "Updated the profile image";
        if (path.contains("/profile/update-profile")) return "Updated profile information";
        if (path.contains("/profile/dependent-list-view")) return "Viewed dependent details";
        if (path.contains("/profile/dependent")) return "Viewed a dependent profile";
        if (path.contains("/profile/policy-document")) return "Viewed a policy document";
        if (path.contains("/profile")) return "Viewed profile information";
        if (path.contains("/sign-up")) return "Registered or verified a WeCare App account";
        if (path.contains("/insurance/request")) return "Submitted a medical insurance claim";
        if (path.contains("/insurance/filter-list")) return "Viewed the medical claim list";
        if (path.contains("/insurance/find")) return "Viewed a medical claim";
        if (path.contains("/insurance/reference-data")) return "Opened the medical claim form";
        if (path.contains("/death/request")) return "Submitted a death donation claim";
        if (path.contains("/death/filter-list")) return "Viewed the death claim list";
        if (path.contains("/death/find")) return "Viewed a death claim";
        if (path.contains("/death/reference-data")) return "Opened the death claim form";
        if (path.contains("/dashboard")) return "Viewed the WeCare App dashboard";
        if (path.contains("/assisted/employee-select")) return "Selected an employee for an assisted claim";
        if (path.contains("/document/upload")) return "Uploaded a claim document";
        if (path.contains("/document/download")) return "Downloaded a claim document";
        if (path.contains("/document/find")) return "Viewed a claim document";
        if (path.contains("/in-app/read")) return "Marked a notification as read";
        if (path.contains("/in-app/filter-list")) return "Viewed notifications";
        return "Performed an activity in " + moduleDescription(log);
    }

    private String moduleDescription(AuditLog log) {
        if (ADMIN_SOURCE.equals(log.getSource()))
            return log.getPage() == null ? "WeCare Admin" : log.getPage().getDescription();
        return switch (log.getModule() == null ? "" : log.getModule()) {
            case "login-service" -> "Login and Password";
            case "auth-service" -> "Profile and Account";
            case "claim-service" -> "Claims";
            case "document-service" -> "Documents";
            case "notification-service" -> "Notifications";
            default -> "WeCare App";
        };
    }

    private List<SimpleBaseDTO> appModules() {
        return List.of(new SimpleBaseDTO("login-service", "Login and Password"),
                new SimpleBaseDTO("auth-service", "Profile and Account"),
                new SimpleBaseDTO("claim-service", "Claims"),
                new SimpleBaseDTO("document-service", "Documents"),
                new SimpleBaseDTO("notification-service", "Notifications"));
    }

    private String resolveResult(AuditLog log) {
        return log.getResult() == null ? "SUCCESS" : log.getResult();
    }

    private String emptyAsSystem(String username) {
        return username == null || username.isBlank() ? "System" : username;
    }

    private String readable(String value) {
        String text = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void normalizeSort(PaginationRequest<AuditLogSearchDTO> request) {
        if (!List.of("id", "createdDate", "module", "action", "result", "createdBy", "ipAddress",
                        "clientAppVersion", "clientPlatform", "appUpdateStatus")
                .contains(request.getSortColumn())) request.setSortColumn("createdDate");
    }

    private ResponseEntity<ApiResponse<Object>> unauthorized() {
        return ResponseEntity.ok(responseUtil.error(null, 1003,
                "You are not authorized to access this activity log"));
    }
}

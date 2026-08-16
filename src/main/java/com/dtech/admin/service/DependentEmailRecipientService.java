package com.dtech.admin.service;

import com.dtech.admin.enums.*;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.EmailNotificationEvent;
import com.dtech.admin.model.EmailNotificationRecipientRule;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.EmailNotificationEventRepository;
import com.dtech.admin.repository.EmailNotificationRecipientRuleRepository;
import com.dtech.admin.repository.WebUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class DependentEmailRecipientService {
    private static final Set<String> LEGACY_ADMIN_ROLE_CODES = Set.of(
            "SUPERADMIN1", "SUPERADMIN", "ADMIN", "APPROVER", "DevTest", "SubAdmin"
    );

    private final EmailNotificationEventRepository eventRepository;
    private final EmailNotificationRecipientRuleRepository ruleRepository;
    private final WebUserRepository webUserRepository;

    public List<WebUser> resolve(DependentEmailEvent event, ClaimsDependents dependent) {
        String companyCode = resolveCompanyCode(dependent);
        try {
            Optional<EmailNotificationEvent> configuredEvent = eventRepository.findByCode(event.name());
            if (configuredEvent.isEmpty()) return legacyFallback(event, companyCode, "event is not configured");
            if (!Status.ACTIVE.equals(configuredEvent.get().getStatus())) return List.of();

            List<EmailNotificationRecipientRule> rules = ruleRepository
                    .findAllByEvent_CodeAndEvent_StatusAndStatus(event.name(), Status.ACTIVE, Status.ACTIVE);
            if (rules.isEmpty()) return List.of();

            Map<String, WebUser> recipients = new LinkedHashMap<>();
            for (EmailNotificationRecipientRule rule : rules) {
                resolveRule(rule, companyCode).forEach(user -> recipients.putIfAbsent(recipientKey(user), user));
            }
            return new ArrayList<>(recipients.values());
        } catch (RuntimeException ex) {
            log.error("Unable to load DB recipients for dependent email event {}. Using existing role routing.", event, ex);
            return legacyFallback(event, companyCode, "configuration lookup failed");
        }
    }

    private List<WebUser> resolveRule(EmailNotificationRecipientRule rule, String companyCode) {
        if (rule == null || rule.getRecipientType() == null || !StringUtils.hasText(rule.getRecipientCode())) return List.of();
        String code = rule.getRecipientCode().trim();
        List<WebUser> candidates = switch (rule.getRecipientType()) {
            case USER_ROLE -> webUserRepository.findAllByUserRole_CodeIgnoreCaseAndStatus(code, Status.ACTIVE);
            case SPECIFIC_USER -> webUserRepository.findByUsernameIgnoreCaseAndStatus(code, Status.ACTIVE)
                    .map(List::of).orElseGet(List::of);
            case APPROVAL_LEVEL -> resolveApprovalLevel(code);
        };
        return applyCompanyScope(candidates, rule.getCompanyScope(), companyCode);
    }

    private List<WebUser> resolveApprovalLevel(String code) {
        try {
            return webUserRepository.findAllByApprovalLevelAndStatus(
                    ApprovalLevel.valueOf(code.toUpperCase(Locale.ROOT)), Status.ACTIVE);
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring invalid dependent email approval level {}", code);
            return List.of();
        }
    }

    private List<WebUser> applyCompanyScope(List<WebUser> users, EmailCompanyScope scope, String companyCode) {
        if (users == null || users.isEmpty()) return List.of();
        if (scope == null || EmailCompanyScope.ALL_COMPANIES.equals(scope) || !StringUtils.hasText(companyCode)) return users;
        return users.stream().filter(user -> {
            boolean unassigned = user.getCompanies() == null || user.getCompanies().isEmpty();
            boolean sameCompany = !unassigned && user.getCompanies().stream().anyMatch(company -> company != null
                    && Status.ACTIVE.equals(company.getStatus()) && companyCode.equalsIgnoreCase(company.getCode()));
            return EmailCompanyScope.SAME_COMPANY_OR_UNASSIGNED.equals(scope) ? unassigned || sameCompany : sameCompany;
        }).toList();
    }

    private List<WebUser> legacyFallback(DependentEmailEvent event, String companyCode, String reason) {
        log.warn("Using legacy recipient routing for dependent email event {} because {}", event, reason);
        return webUserRepository.findAllByStatus(Status.ACTIVE).stream()
                .filter(user -> user.getUserRole() != null && StringUtils.hasText(user.getUserRole().getCode()))
                .filter(user -> LEGACY_ADMIN_ROLE_CODES.stream()
                        .anyMatch(role -> role.equalsIgnoreCase(user.getUserRole().getCode())))
                .filter(user -> !StringUtils.hasText(companyCode)
                        || user.getCompanies() == null
                        || user.getCompanies().isEmpty()
                        || user.getCompanies().stream().anyMatch(company -> company != null
                        && Status.ACTIVE.equals(company.getStatus()) && companyCode.equalsIgnoreCase(company.getCode())))
                .toList();
    }

    private String resolveCompanyCode(ClaimsDependents dependent) {
        return Optional.ofNullable(dependent)
                .map(ClaimsDependents::getApplicationUser)
                .map(user -> user.getUserPersonalDetails())
                .map(personal -> personal.getUserCompanyDetails())
                .map(details -> details.getCompanyTypes())
                .map(company -> company.getCode())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(null);
    }

    private String recipientKey(WebUser user) {
        if (user == null) return UUID.randomUUID().toString();
        if (StringUtils.hasText(user.getEmail())) return "email:" + user.getEmail().trim().toLowerCase(Locale.ROOT);
        if (user.getId() != null) return "id:" + user.getId();
        return "username:" + Objects.toString(user.getUsername(), UUID.randomUUID().toString()).toLowerCase(Locale.ROOT);
    }
}

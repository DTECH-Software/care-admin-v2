package com.dtech.admin.service;

import com.dtech.admin.enums.*;
import com.dtech.admin.model.EmailNotificationEvent;
import com.dtech.admin.model.EmailNotificationRecipientRule;
import com.dtech.admin.model.SupportTicket;
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
public class SupportTicketEmailRecipientService {
    public static final String TICKET_CREATOR = "TICKET_CREATOR";

    private final EmailNotificationEventRepository eventRepository;
    private final EmailNotificationRecipientRuleRepository ruleRepository;
    private final WebUserRepository webUserRepository;

    public List<WebUser> resolve(SupportTicketEmailEvent event, SupportTicket ticket, String actorUsername) {
        try {
            Optional<EmailNotificationEvent> configuredEvent = eventRepository.findByCode(event.name());
            if (configuredEvent.isEmpty()) {
                log.warn("Support-ticket email event {} is not configured; notification was not sent", event);
                return List.of();
            }
            if (!Status.ACTIVE.equals(configuredEvent.get().getStatus())) return List.of();

            List<EmailNotificationRecipientRule> rules = ruleRepository
                    .findAllByEvent_CodeAndEvent_StatusAndStatus(event.name(), Status.ACTIVE, Status.ACTIVE);
            Map<String, WebUser> recipients = new LinkedHashMap<>();
            for (EmailNotificationRecipientRule rule : rules) {
                resolveRule(rule, ticket).forEach(user -> recipients.putIfAbsent(recipientKey(user), user));
            }
            if (StringUtils.hasText(actorUsername)) {
                recipients.values().removeIf(user -> user != null && StringUtils.hasText(user.getUsername())
                        && actorUsername.trim().equalsIgnoreCase(user.getUsername().trim()));
            }
            return new ArrayList<>(recipients.values());
        } catch (RuntimeException ex) {
            log.error("Unable to load recipients for support-ticket email event {}; notification was not sent", event, ex);
            return List.of();
        }
    }

    private List<WebUser> resolveRule(EmailNotificationRecipientRule rule, SupportTicket ticket) {
        if (rule == null || rule.getRecipientType() == null || !StringUtils.hasText(rule.getRecipientCode())) {
            return List.of();
        }
        String code = rule.getRecipientCode().trim();
        List<WebUser> candidates = switch (rule.getRecipientType()) {
            case USER_ROLE -> webUserRepository.findAllByUserRole_CodeIgnoreCaseAndStatus(code, Status.ACTIVE);
            case SPECIFIC_USER -> findActiveUser(code);
            case APPROVAL_LEVEL -> resolveApprovalLevel(code);
            case EVENT_USER -> resolveEventUser(code, ticket);
        };
        return applyCompanyScope(candidates, rule.getCompanyScope(), companyCode(ticket));
    }

    private List<WebUser> resolveEventUser(String code, SupportTicket ticket) {
        if (!TICKET_CREATOR.equalsIgnoreCase(code) || ticket == null || !StringUtils.hasText(ticket.getCreatedBy())) {
            return List.of();
        }
        return findActiveUser(ticket.getCreatedBy());
    }

    private List<WebUser> findActiveUser(String username) {
        return webUserRepository.findByUsernameIgnoreCaseAndStatus(username, Status.ACTIVE)
                .map(List::of).orElseGet(List::of);
    }

    private List<WebUser> resolveApprovalLevel(String code) {
        try {
            return webUserRepository.findAllByApprovalLevelAndStatus(
                    ApprovalLevel.valueOf(code.toUpperCase(Locale.ROOT)), Status.ACTIVE);
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring invalid support-ticket email approval level {}", code);
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

    private String companyCode(SupportTicket ticket) {
        return Optional.ofNullable(ticket).map(SupportTicket::getCompany).map(company -> company.getCode())
                .filter(StringUtils::hasText).map(String::trim).orElse(null);
    }

    private String recipientKey(WebUser user) {
        if (user == null) return UUID.randomUUID().toString();
        if (StringUtils.hasText(user.getEmail())) return "email:" + user.getEmail().trim().toLowerCase(Locale.ROOT);
        if (user.getId() != null) return "id:" + user.getId();
        return "username:" + Objects.toString(user.getUsername(), UUID.randomUUID().toString()).toLowerCase(Locale.ROOT);
    }
}

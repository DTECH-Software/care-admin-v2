package com.dtech.admin.service;

import com.dtech.admin.enums.*;
import com.dtech.admin.model.EmailNotificationEvent;
import com.dtech.admin.model.EmailNotificationRecipientRule;
import com.dtech.admin.model.InsuranceClaimsRequest;
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
public class ClaimEmailRecipientService {
    private final EmailNotificationEventRepository eventRepository;
    private final EmailNotificationRecipientRuleRepository ruleRepository;
    private final WebUserRepository webUserRepository;

    public List<WebUser> resolve(ClaimEmailEvent event,
                                 InsuranceClaimsRequest claim,
                                 ApprovalLevel fallbackApprovalLevel) {
        try {
            Optional<EmailNotificationEvent> configuredEvent = eventRepository.findByCode(event.name());
            if (configuredEvent.isEmpty()) {
                return fallback(event, fallbackApprovalLevel, "event is not configured");
            }
            if (!Status.ACTIVE.equals(configuredEvent.get().getStatus())) {
                log.info("Claim email event {} is inactive", event);
                return List.of();
            }

            List<EmailNotificationRecipientRule> rules = ruleRepository
                    .findAllByEvent_CodeAndEvent_StatusAndStatus(event.name(), Status.ACTIVE, Status.ACTIVE);
            if (rules.isEmpty()) {
                log.warn("No active recipient rules configured for claim email event {}", event);
                return List.of();
            }

            String companyCode = resolveCompanyCode(claim);
            Map<String, WebUser> uniqueRecipients = new LinkedHashMap<>();
            for (EmailNotificationRecipientRule rule : rules) {
                resolveRule(rule, companyCode).forEach(user -> uniqueRecipients.putIfAbsent(recipientKey(user), user));
            }
            return new ArrayList<>(uniqueRecipients.values());
        } catch (RuntimeException ex) {
            log.error("Unable to load database recipient configuration for claim email event {}. Using existing approval-level routing.",
                    event, ex);
            return fallback(event, fallbackApprovalLevel, "configuration lookup failed");
        }
    }

    private List<WebUser> resolveRule(EmailNotificationRecipientRule rule, String companyCode) {
        if (rule == null || rule.getRecipientType() == null || !StringUtils.hasText(rule.getRecipientCode())) {
            return List.of();
        }

        String recipientCode = rule.getRecipientCode().trim();
        boolean sameCompany = EmailCompanyScope.SAME_COMPANY.equals(rule.getCompanyScope());
        if (sameCompany && !StringUtils.hasText(companyCode)) {
            log.warn("Skipping SAME_COMPANY email rule {} because the claim company is missing", rule.getId());
            return List.of();
        }

        return switch (rule.getRecipientType()) {
            case APPROVAL_LEVEL -> resolveApprovalLevel(recipientCode, sameCompany, companyCode);
            case USER_ROLE -> sameCompany
                    ? webUserRepository.findAllByUserRole_CodeIgnoreCaseAndStatusAndCompanies_CodeIgnoreCase(
                    recipientCode, Status.ACTIVE, companyCode)
                    : webUserRepository.findAllByUserRole_CodeIgnoreCaseAndStatus(recipientCode, Status.ACTIVE);
            case SPECIFIC_USER -> {
                Optional<WebUser> user = sameCompany
                        ? webUserRepository.findByUsernameIgnoreCaseAndStatusAndCompanies_CodeIgnoreCase(
                        recipientCode, Status.ACTIVE, companyCode)
                        : webUserRepository.findByUsernameIgnoreCaseAndStatus(recipientCode, Status.ACTIVE);
                yield user.map(List::of).orElseGet(List::of);
            }
        };
    }

    private List<WebUser> resolveApprovalLevel(String recipientCode, boolean sameCompany, String companyCode) {
        try {
            ApprovalLevel approvalLevel = ApprovalLevel.valueOf(recipientCode.toUpperCase(Locale.ROOT));
            return sameCompany
                    ? webUserRepository.findAllByApprovalLevelAndStatusAndCompanies_CodeIgnoreCase(
                    approvalLevel, Status.ACTIVE, companyCode)
                    : webUserRepository.findAllByApprovalLevelAndStatus(approvalLevel, Status.ACTIVE);
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring invalid approval level {} in email recipient configuration", recipientCode);
            return List.of();
        }
    }

    private List<WebUser> fallback(ClaimEmailEvent event,
                                   ApprovalLevel fallbackApprovalLevel,
                                   String reason) {
        if (fallbackApprovalLevel == null) {
            return List.of();
        }
        log.warn("Using legacy recipient routing for claim email event {} because {}", event, reason);
        return webUserRepository.findAllByApprovalLevelAndStatus(fallbackApprovalLevel, Status.ACTIVE);
    }

    private String resolveCompanyCode(InsuranceClaimsRequest claim) {
        return Optional.ofNullable(claim)
                .map(InsuranceClaimsRequest::getEmployee)
                .map(employee -> employee.getUserPersonalDetails())
                .map(personal -> personal.getUserCompanyDetails())
                .map(company -> company.getCompanyTypes())
                .map(type -> type.getCode())
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

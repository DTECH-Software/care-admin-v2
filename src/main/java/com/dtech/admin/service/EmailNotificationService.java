package com.dtech.admin.service;

import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.WebUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Sends email notifications associated with claim approvals/escalations.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class EmailNotificationService {

    @Autowired
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String mailFrom;

    public void notifyLevelTwoPendingApproval(List<WebUser> recipients, InsuranceClaimsRequest claim, Locale locale) {
        if (CollectionUtils.isEmpty(recipients)) {
            log.info("No level 02 recipients found to notify");
            return;
        }

        Set<String> processedEmails = new HashSet<>();
        for (WebUser recipient : recipients) {
            sendMail(recipient, buildLevelTwoPendingSubject(claim), buildLevelTwoPendingBody(recipient, claim, locale),
                    "level 02 escalation", processedEmails);
        }
    }

    /**
     * Sends pending approval email to Level 03 after a Level 02 approve (e.g., when Level 01 had rejected).
     */
    public void notifyLevelThreePendingApproval(List<WebUser> recipients, InsuranceClaimsRequest claim, Locale locale) {
        if (CollectionUtils.isEmpty(recipients)) {
            log.info("No level 03 recipients found to notify");
            return;
        }

        Set<String> processedEmails = new HashSet<>();
        for (WebUser recipient : recipients) {
            sendMail(recipient, buildLevelThreePendingSubject(claim), buildLevelThreePendingBody(claim, locale),
                    "level 03 escalation", processedEmails);
        }
    }

    public void notifyLevelTwoRejection(List<WebUser> levelThreeRecipients,
                                        InsuranceClaimsRequest claim,
                                        String remark,
                                        Locale locale) {
        if (CollectionUtils.isEmpty(levelThreeRecipients)) {
            log.info("No Level 03 recipients available to notify for Level 02 rejection of claim {}", claim.getRequestId());
            return;
        }

        String subject = "Claim %s rejected at Level 02".formatted(claim.getRequestId());
        Set<String> notifiedEmails = new HashSet<>();
        BigDecimal levelOneApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL01);
        BigDecimal levelTwoApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL02);

        for (WebUser recipient : levelThreeRecipients) {
            sendMail(recipient, subject, buildLevelTwoRejectionBody(recipient, claim, remark, locale, levelOneApproved, levelTwoApproved),
                    "level 02 rejection", notifiedEmails);
        }
    }

    /**
     * Sends Level 02 rejection details back to Level 01 approvers (when Level 01 had already rejected).
     */
    public void notifyLevelOneOnLevelTwoRejection(List<WebUser> recipients,
                                                  InsuranceClaimsRequest claim,
                                                  String remark,
                                                  Locale locale) {
        if (CollectionUtils.isEmpty(recipients)) {
            log.info("No Level 01 recipients available to notify for Level 02 rejection of claim {}", claim.getRequestId());
            return;
        }

        String subject = "Claim %s rejected at Level 02".formatted(claim.getRequestId());
        Set<String> processedEmails = new HashSet<>();

        Workflow levelOneStatus = findStatusByLevel(claim, ApprovalLevel.LEVEL01);
        BigDecimal levelOneApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL01);
        BigDecimal levelTwoApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL02);

        for (WebUser recipient : recipients) {
            sendMail(recipient, subject,
                    buildLevelOneRejectionAfterLevelTwoBody(claim, remark, locale, levelOneStatus, levelOneApproved, levelTwoApproved),
                    "level 01 rejection after level 02", processedEmails);
        }
    }

    public void notifyLevelOneOnApproval(List<WebUser> recipients,
                                         InsuranceClaimsRequest claim,
                                         BigDecimal approvedAmount,
                                         ApprovalLevel approvedBy,
                                         Locale locale) {
        if (CollectionUtils.isEmpty(recipients)) {
            log.info("No Level 01 recipients available to notify for {} approval of claim {}", approvedBy, claim.getRequestId());
            return;
        }

        String subject = "Claim %s approved at %s".formatted(claim.getRequestId(), formatLevel(approvedBy));
        BigDecimal amountToUse = approvedAmount != null
                ? approvedAmount
                : (claim.getApprovedAmount() != null ? claim.getApprovedAmount() : claim.getRequestAmount());
        String formattedAmount = formatAmount(amountToUse != null ? amountToUse : BigDecimal.ZERO, locale);

        Set<String> processedEmails = new HashSet<>();
        for (WebUser recipient : recipients) {
            sendMail(recipient, subject, buildApprovalBody(recipient, claim, formattedAmount, approvedBy, locale),
                    "level 01 notification", processedEmails);
        }
    }

    /**
     * Sends Level 01 rejection notification to Level 02 approvers with full details.
     */
    public void notifyLevelTwoOnLevelOneRejection(List<WebUser> recipients,
                                                  InsuranceClaimsRequest claim,
                                                  String remark,
                                                  Locale locale) {
        if (CollectionUtils.isEmpty(recipients)) {
            log.info("No Level 02 recipients available to notify for Level 01 rejection of claim {}", claim.getRequestId());
            return;
        }

        String subject = "Claim %s rejected at Level 01".formatted(claim.getRequestId());
        Set<String> processedEmails = new HashSet<>();

        BigDecimal levelOneApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL01);
        String latestRemark = findLatestRemark(claim);

        for (WebUser recipient : recipients) {
            sendMail(recipient, subject,
                    buildLevelTwoInfoAfterLevelOneRejectionBody(claim, remark, locale, levelOneApproved, latestRemark),
                    "level 02 info after level 01 rejection", processedEmails);
        }
    }

    /**
     * Sends final decision (approve/reject) from Level 03 to Level 01 approvers with full approval details.
     */
    public void notifyLevelOneFinalDecision(List<WebUser> recipients,
                                            InsuranceClaimsRequest claim,
                                            Workflow finalStatus,
                                            String remark,
                                            Locale locale) {
        if (CollectionUtils.isEmpty(recipients)) {
            log.info("No Level 01 recipients available to notify for final decision of claim {}", claim.getRequestId());
            return;
        }

        String statusText = Workflow.APPROVED.equals(finalStatus) ? "approved" : "rejected";
        String subject = "Claim %s %s at Level 03".formatted(claim.getRequestId(), statusText);

        BigDecimal levelOneApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL01);
        BigDecimal levelTwoApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL02);
        BigDecimal levelThreeApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL03);

        Set<String> processedEmails = new HashSet<>();
        for (WebUser recipient : recipients) {
            sendMail(recipient, subject,
                    buildLevelOneFinalDecisionBody(claim, finalStatus, remark, locale,
                            levelOneApproved, levelTwoApproved, levelThreeApproved),
                    "level 01 final decision", processedEmails);
        }
    }

    private void sendMail(WebUser recipient, String subject, String body, String logContext, Set<String> processedEmails) {
        if (recipient == null || !StringUtils.hasText(recipient.getEmail())) {
            log.warn("Skipping {} recipient - missing email", logContext);
            return;
        }

        String email = recipient.getEmail().trim();
        if (processedEmails != null) {
            String normalizedEmail = email.toLowerCase(Locale.ROOT);
            if (!processedEmails.add(normalizedEmail)) {
                return;
            }
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (StringUtils.hasText(mailFrom)) {
                message.setFrom(mailFrom);
            }
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent {} email to {}", logContext, email);
        } catch (Exception ex) {
            log.error("Failed to send {} email to {}", logContext, email, ex);
        }
    }

    private String buildLevelTwoPendingSubject(InsuranceClaimsRequest claim) {
        return "Claim %s requires Level 02 approval".formatted(claim.getRequestId());
    }

    private String buildLevelThreePendingSubject(InsuranceClaimsRequest claim) {
        return "Claim %s requires Level 03 approval".formatted(claim.getRequestId());
    }

    private String buildLevelTwoPendingBody(WebUser recipient, InsuranceClaimsRequest claim, Locale locale) {
        BigDecimal requestAmount = claim.getRequestAmount() != null ? claim.getRequestAmount() : BigDecimal.ZERO;
        BigDecimal levelOneApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL01);
        Workflow levelOneStatus = findStatusByLevel(claim, ApprovalLevel.LEVEL01);
        String latestRemark = findLatestRemark(claim);
        return """
                Dear Team,

                Claim %s submitted by %s has been processed by Level 01 and now awaits your review.
                Requested amount: %s
                %s
                %s

                Please log into the admin portal to continue the approval process.
                https://wecare-admin.dsi.lk/care-admin
                
                Thanks & Best Regards!!,
                WeCare Team.
                """.formatted(claim.getRequestId(), getEmployeeName(claim),
                formatAmount(requestAmount, locale),
                formatRemarkLine(latestRemark),
                formatLevelOneDecisionLine(levelOneStatus, levelOneApproved, locale));
    }

    private String buildLevelThreePendingBody(InsuranceClaimsRequest claim, Locale locale) {
        BigDecimal requestAmount = claim.getRequestAmount() != null ? claim.getRequestAmount() : BigDecimal.ZERO;
        BigDecimal levelOneApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL01);
        BigDecimal levelTwoApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL02);
        Workflow levelOneStatus = findStatusByLevel(claim, ApprovalLevel.LEVEL01);
        String latestRemark = findLatestRemark(claim);

        StringBuilder body = new StringBuilder("""
                Dear Team,

                Claim %s submitted by %s has progressed to Level 03 for your approval.
                """.formatted(claim.getRequestId(), getEmployeeName(claim)));

        body.append("Requested amount: ").append(formatAmount(requestAmount, locale)).append(System.lineSeparator());

        if (levelOneStatus != null) {
            body.append("Level 01 decision: ").append(levelOneStatus.name()).append(System.lineSeparator());
        }
        if (levelOneApproved != null) {
            body.append("Level 01 approved amount: ").append(formatAmount(levelOneApproved, locale)).append(System.lineSeparator());
        }
        if (levelTwoApproved != null) {
            body.append("Level 02 approved amount: ").append(formatAmount(levelTwoApproved, locale)).append(System.lineSeparator());
        }

        appendRemark(body, latestRemark);

        body.append(System.lineSeparator());
        body.append("""
                Please log into the admin portal to continue the approval process.
                https://wecare-admin.dsi.lk/care-admin
                
                Thanks & Best Regards!!,
                WeCare Team.
                """);

        return body.toString();
    }

    private String buildLevelTwoInfoAfterLevelOneRejectionBody(InsuranceClaimsRequest claim,
                                                               String remark,
                                                               Locale locale,
                                                               BigDecimal levelOneApproved,
                                                               String latestRemark) {
        StringBuilder body = new StringBuilder("""
                Dear Team,

                Claim %s submitted by %s was rejected at Level 01.
                """.formatted(claim.getRequestId(), getEmployeeName(claim)));

        BigDecimal requestAmount = claim.getRequestAmount() != null ? claim.getRequestAmount() : BigDecimal.ZERO;
        body.append("Requested amount: ").append(formatAmount(requestAmount, locale)).append(System.lineSeparator());
        body.append("Level 01 decision: REJECTED").append(System.lineSeparator());
        if (levelOneApproved != null) {
            body.append("Level 01 approved amount: ").append(formatAmount(levelOneApproved, locale)).append(System.lineSeparator());
        }

        appendRemark(body, StringUtils.hasText(remark) ? remark : latestRemark);

        body.append("""
                
                This is for your information.
                
                Please log into the admin portal to review the claim if further action is required.
                https://wecare-admin.dsi.lk/care-admin
                
                Thanks & Best Regards!!,
                WeCare Team.
                """);

        return body.toString();
    }

    private String buildLevelTwoRejectionBody(WebUser recipient,
                                              InsuranceClaimsRequest claim,
                                              String remark,
                                              Locale locale,
                                              BigDecimal levelOneApproved,
                                              BigDecimal levelTwoApproved) {
        StringBuilder body = new StringBuilder("""
                Dear Team,

                Claim %s submitted by %s was rejected at Level 02.
                """.formatted(claim.getRequestId(), getEmployeeName(claim)));

        BigDecimal requestAmount = claim.getRequestAmount() != null ? claim.getRequestAmount() : BigDecimal.ZERO;
        body.append("Requested amount: ").append(formatAmount(requestAmount, locale)).append(System.lineSeparator());

        if (levelOneApproved != null) {
            body.append("Level 01 approved amount: ").append(formatAmount(levelOneApproved, locale)).append(System.lineSeparator());
        }
        body.append("Level 02 decision: Rejected").append(System.lineSeparator());
        if (levelTwoApproved != null) {
            body.append("Level 02 approved amount: ").append(formatAmount(levelTwoApproved, locale)).append(System.lineSeparator());
        }

        String latestRemark = findLatestRemark(claim);
        appendRemark(body, StringUtils.hasText(remark) ? remark : latestRemark);

        body.append(System.lineSeparator());
        body.append("""
                Please log into the admin portal to review the claim if further action is required.
                https://wecare-admin.dsi.lk/care-admin
                
                Thanks & Best Regards!!,
                WeCare Team.
                """);

        return body.toString();
    }

    private String buildApprovalBody(WebUser recipient, InsuranceClaimsRequest claim, String formattedAmount, ApprovalLevel approvedBy, Locale locale) {
        BigDecimal levelOneApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL01);
        BigDecimal levelTwoApproved = findApprovedAmountByLevel(claim, ApprovalLevel.LEVEL02);
        String latestRemark = findLatestRemark(claim);

        StringBuilder body = new StringBuilder("""
                Dear Team,

                Claim %s submitted by %s has been approved at %s.
                """.formatted(claim.getRequestId(), getEmployeeName(claim), formatLevel(approvedBy)));

        if (levelOneApproved != null) {
            body.append("Level 01 approved amount: ").append(formatAmount(levelOneApproved, locale)).append(System.lineSeparator());
        }
        if (levelTwoApproved != null) {
            body.append("Level 02 approved amount: ").append(formatAmount(levelTwoApproved, locale)).append(System.lineSeparator());
        }

        body.append("Approved amount: ").append(formattedAmount).append(System.lineSeparator()).append(System.lineSeparator());
        appendRemark(body, latestRemark);

        body.append("""
                This is for your information.
                
                Please log into the admin portal to review the claim if further action is required.
                https://wecare-admin.dsi.lk/care-admin
                
                Thanks & Best Regards!!,
                WeCare Team.
                """);

        return body.toString();
    }

    private String buildLevelOneFinalDecisionBody(InsuranceClaimsRequest claim,
                                                  Workflow finalStatus,
                                                  String remark,
                                                  Locale locale,
                                                  BigDecimal levelOneApproved,
                                                  BigDecimal levelTwoApproved,
                                                  BigDecimal levelThreeApproved) {

        Workflow levelOneStatus = findStatusByLevel(claim, ApprovalLevel.LEVEL01);
        Workflow levelTwoStatus = findStatusByLevel(claim, ApprovalLevel.LEVEL02);
        Workflow levelThreeStatus = findStatusByLevel(claim, ApprovalLevel.LEVEL03);
        String latestRemark = findLatestRemark(claim);

        StringBuilder body = new StringBuilder("""
                Dear Team,

                Claim %s submitted by %s has been %s at Level 03.
                """.formatted(claim.getRequestId(), getEmployeeName(claim),
                Workflow.APPROVED.equals(finalStatus) ? "approved" : "rejected"));

        BigDecimal requestAmount = claim.getRequestAmount() != null ? claim.getRequestAmount() : BigDecimal.ZERO;
        body.append("Requested amount: ").append(formatAmount(requestAmount, locale)).append(System.lineSeparator());

        if (levelOneStatus != null) {
            body.append("Level 01 decision: ").append(levelOneStatus.name()).append(System.lineSeparator());
        }
        if (levelOneApproved != null) {
            body.append("Level 01 approved amount: ").append(formatAmount(levelOneApproved, locale)).append(System.lineSeparator());
        }
        if (levelTwoStatus != null) {
            body.append("Level 02 decision: ").append(levelTwoStatus.name()).append(System.lineSeparator());
        }
        if (levelTwoApproved != null) {
            body.append("Level 02 approved amount: ").append(formatAmount(levelTwoApproved, locale)).append(System.lineSeparator());
        }
        if (levelThreeStatus != null) {
            body.append("Level 03 decision: ").append(levelThreeStatus.name()).append(System.lineSeparator());
        }
        if (Workflow.APPROVED.equals(finalStatus) && levelThreeApproved != null) {
            body.append("Level 03 approved amount: ").append(formatAmount(levelThreeApproved, locale)).append(System.lineSeparator());
        }

        if (StringUtils.hasText(remark) || StringUtils.hasText(latestRemark)) {
            appendRemark(body, StringUtils.hasText(remark) ? remark : latestRemark);
        }

        body.append(System.lineSeparator());
        body.append("""
                This is for your information.
                
                Please log into the admin portal to review the claim if further action is required.
                https://wecare-admin.dsi.lk/care-admin
                
                Thanks & Best Regards!!,
                WeCare Team.
                """);

        return body.toString();
    }

    private String buildLevelOneRejectionAfterLevelTwoBody(InsuranceClaimsRequest claim,
                                                           String remark,
                                                           Locale locale,
                                                           Workflow levelOneStatus,
                                                           BigDecimal levelOneApproved,
                                                           BigDecimal levelTwoApproved) {
        StringBuilder body = new StringBuilder("""
                Dear Team,

                Claim %s submitted by %s was rejected at Level 02.
                """.formatted(claim.getRequestId(), getEmployeeName(claim)));

        BigDecimal requestAmount = claim.getRequestAmount() != null ? claim.getRequestAmount() : BigDecimal.ZERO;
        body.append("Requested amount: ").append(formatAmount(requestAmount, locale)).append(System.lineSeparator());

        if (levelOneStatus != null) {
            if (Workflow.APPROVED.equals(levelOneStatus) && levelOneApproved != null) {
                body.append("Level 01 approved amount: ").append(formatAmount(levelOneApproved, locale)).append(System.lineSeparator());
            } else if (Workflow.REJECTED.equals(levelOneStatus)) {
                body.append("Level 01 decision: Rejected").append(System.lineSeparator());
            }
        }

        body.append("Level 02 decision: Rejected").append(System.lineSeparator());
        if (levelTwoApproved != null) {
            body.append("Level 02 approved amount: ").append(formatAmount(levelTwoApproved, locale)).append(System.lineSeparator());
        }

        String latestRemark = findLatestRemark(claim);
        appendRemark(body, StringUtils.hasText(remark) ? remark : latestRemark);

        body.append(System.lineSeparator());
        body.append("""
                This is for your information.
                
                Please log into the admin portal to review the claim if further action is required.
                https://wecare-admin.dsi.lk/care-admin
                
                Thanks & Best Regards!!,
                WeCare Team.
                """);

        return body.toString();
    }

    private BigDecimal findApprovedAmountByLevel(InsuranceClaimsRequest claim, ApprovalLevel level) {
        if (claim.getApprovalWorkFlows() == null) {
            return null;
        }
        return claim.getApprovalWorkFlows().stream()
                .filter(wf -> level.equals(wf.getApprovalLevel()))
                .filter(wf -> Workflow.APPROVED.equals(wf.getStatus()))
                .map(ApprovalWorkFlow::getApprovedAmount)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String findLatestRemark(InsuranceClaimsRequest claim) {
        if (claim.getApprovalWorkFlows() == null) {
            return "";
        }
        return claim.getApprovalWorkFlows().stream()
                .filter(wf -> StringUtils.hasText(wf.getRejectedRemark()))
                .max(Comparator.comparing(ApprovalWorkFlow::getApprovedDate, Comparator.nullsLast(Date::compareTo)))
                .map(ApprovalWorkFlow::getRejectedRemark)
                .orElse("");
    }

    private void appendRemark(StringBuilder body, String remark) {
        String trimmed = remark != null ? remark.trim() : "";
        if (!trimmed.isEmpty()) {
            body.append("Remark: ").append(trimmed).append(System.lineSeparator()).append(System.lineSeparator());
        }
    }

    private String formatRemarkLine(String remark) {
        String trimmed = remark != null ? remark.trim() : "";
        return trimmed.isEmpty() ? "" : "Remark: " + trimmed;
    }

    private String formatLevelOneDecisionLine(Workflow levelOneStatus, BigDecimal levelOneApproved, Locale locale) {
        if (levelOneStatus == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Level 01 decision: ").append(levelOneStatus.name());
        if (Workflow.APPROVED.equals(levelOneStatus) && levelOneApproved != null) {
            sb.append(" (").append(formatAmount(levelOneApproved, locale)).append(")");
        }
        return sb.toString();
    }

    private Workflow findStatusByLevel(InsuranceClaimsRequest claim, ApprovalLevel level) {
        if (claim.getApprovalWorkFlows() == null) {
            return null;
        }
        return claim.getApprovalWorkFlows().stream()
                .filter(wf -> level.equals(wf.getApprovalLevel()))
                .map(ApprovalWorkFlow::getStatus)
                .findFirst()
                .orElse(null);
    }

    private String getEmployeeName(InsuranceClaimsRequest claim) {
        return claim.getEmployee() != null
                && claim.getEmployee().getUserPersonalDetails() != null
                ? claim.getEmployee().getUserPersonalDetails().getFirstName() + " "
                + claim.getEmployee().getUserPersonalDetails().getLastName()
                : "N/A";
    }

    private String formatAmount(BigDecimal amount, Locale locale) {
        Locale currencyLocale = locale != null ? locale : Locale.forLanguageTag("en-LK");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(currencyLocale);
        currencyFormatter.setCurrency(Currency.getInstance("LKR"));
        return currencyFormatter.format(amount);
    }

    private String formatLevel(ApprovalLevel level) {
        return switch (level) {
            case LEVEL01 -> "Level 01";
            case LEVEL02 -> "Level 02";
            case LEVEL03 -> "Level 03";
        };
    }
}

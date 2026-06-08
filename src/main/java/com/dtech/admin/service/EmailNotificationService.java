package com.dtech.admin.service;

import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.RelationCategory;
import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.model.StaffCategories;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.UserCompanyDetails;
import com.dtech.admin.model.UserPersonalDetails;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.util.ApprovalRemarkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import jakarta.mail.internet.MimeMessage;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
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

    private static final Set<String> ADMIN_NOTIFICATION_ROLE_CODES = Set.of(
            "SUPERADMIN1", "SUPERADMIN", "ADMIN", "APPROVER", "DevTest", "SubAdmin"
    );

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

    public void notifyEmployeeAddedPendingApproval(List<WebUser> recipients,
                                                   UserPersonalDetails employee,
                                                   String hrUsername) {
        if (CollectionUtils.isEmpty(recipients) || employee == null || employee.getUserCompanyDetails() == null) {
            log.info("No recipients or employee details available for employee inclusion notification");
            return;
        }

        String subject = "[%s] Employee inclusion and Claims System Administrator approval Required."
                .formatted(safeValue(employee.getEpfNo()));
        String body = buildEmployeeAddedPendingApprovalBody(employee, hrUsername);

        List<WebUser> adminRecipients = filterAdminNotificationRecipients(recipients);
        if (CollectionUtils.isEmpty(adminRecipients)) {
            log.info("No admin recipients available for employee inclusion notification");
            return;
        }

        Set<String> processedEmails = new HashSet<>();
        for (WebUser recipient : adminRecipients) {
            sendHtmlMail(recipient, subject, body, "employee inclusion pending approval", processedEmails);
        }
    }

    public void notifyEmployeeDeactivated(List<WebUser> recipients,
                                          UserPersonalDetails employee,
                                          String hrUsername) {
        if (CollectionUtils.isEmpty(recipients) || employee == null || employee.getUserCompanyDetails() == null) {
            log.info("No recipients or employee details available for employee deactivation notification");
            return;
        }

        String subject = "[%s] Employee Deactivated".formatted(safeValue(employee.getEpfNo()));
        String body = buildEmployeeDeactivatedBody(employee, hrUsername);

        List<WebUser> adminRecipients = filterAdminNotificationRecipients(recipients);
        if (CollectionUtils.isEmpty(adminRecipients)) {
            log.info("No admin recipients available for employee deactivation notification");
            return;
        }

        Set<String> processedEmails = new HashSet<>();
        for (WebUser recipient : adminRecipients) {
            sendHtmlMail(recipient, subject, body, "employee deactivated", processedEmails);
        }
    }

    public void notifyDependentApprovedByHr(List<WebUser> recipients,
                                            ClaimsDependents dependent,
                                            String hrUsername) {
        if (CollectionUtils.isEmpty(recipients) || dependent == null || dependent.getApplicationUser() == null) {
            log.info("No recipients or dependent details available for dependent approval notification");
            return;
        }

        String subject = "Dependent Approved By HR";
        String body = buildDependentApprovedByHrBody(dependent, hrUsername);

        List<WebUser> adminRecipients = filterAdminNotificationRecipients(recipients);
        if (CollectionUtils.isEmpty(adminRecipients)) {
            log.info("No admin recipients available for dependent approval notification");
            return;
        }

        Set<String> processedEmails = new HashSet<>();
        for (WebUser recipient : adminRecipients) {
            sendHtmlMail(recipient, subject, body, "dependent approved by hr", processedEmails);
        }
    }

    public void notifyCivilStatusApprovedByHr(List<WebUser> recipients,
                                              com.dtech.admin.model.MaritalStatus civilStatusUpdate,
                                              String hrUsername) {
        if (CollectionUtils.isEmpty(recipients) || civilStatusUpdate == null || civilStatusUpdate.getApplicationUser() == null) {
            log.info("No recipients or civil status details available for civil status approval notification");
            return;
        }

        String subject = "Civil Status Approved by HR";
        String body = buildCivilStatusApprovedByHrBody(civilStatusUpdate, hrUsername);

        List<WebUser> adminRecipients = filterAdminNotificationRecipients(recipients);
        if (CollectionUtils.isEmpty(adminRecipients)) {
            log.info("No admin recipients available for civil status approval notification");
            return;
        }

        Set<String> processedEmails = new HashSet<>();
        for (WebUser recipient : adminRecipients) {
            sendHtmlMail(recipient, subject, body, "civil status approved by hr", processedEmails);
        }
    }

    public void notifyStaffCategoryTransferred(List<WebUser> recipients,
                                               UserPersonalDetails employee,
                                               StaffCategories previousStaffCategory,
                                               StaffCategories newStaffCategory,
                                               Date effectiveDate,
                                               String hrUsername) {
        if (CollectionUtils.isEmpty(recipients) || employee == null || employee.getUserCompanyDetails() == null) {
            log.info("No recipients or employee details available for staff category transfer notification");
            return;
        }

        String subject = "Transferring employee data from [%s] to [%s]"
                .formatted(
                        safeValue(previousStaffCategory != null ? previousStaffCategory.getDescription() : null),
                        safeValue(newStaffCategory != null ? newStaffCategory.getDescription() : null)
                );
        String body = buildStaffCategoryTransferredBody(employee, previousStaffCategory, newStaffCategory, effectiveDate, hrUsername);

        List<WebUser> adminRecipients = filterAdminNotificationRecipients(recipients);
        if (CollectionUtils.isEmpty(adminRecipients)) {
            log.info("No admin recipients available for staff category transfer notification");
            return;
        }

        Set<String> processedEmails = new HashSet<>();
        for (WebUser recipient : adminRecipients) {
            sendHtmlMail(recipient, subject, body, "staff category transferred", processedEmails);
        }
    }

    private List<WebUser> filterAdminNotificationRecipients(List<WebUser> recipients) {
        if (CollectionUtils.isEmpty(recipients)) {
            return List.of();
        }
        return recipients.stream()
                .filter(Objects::nonNull)
                .filter(user -> user.getUserRole() != null && StringUtils.hasText(user.getUserRole().getCode()))
                .filter(user -> ADMIN_NOTIFICATION_ROLE_CODES.stream()
                        .anyMatch(roleCode -> roleCode.equalsIgnoreCase(user.getUserRole().getCode())))
                .toList();
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

    private void sendHtmlMail(WebUser recipient, String subject, String body, String logContext, Set<String> processedEmails) {
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            if (StringUtils.hasText(mailFrom)) {
                helper.setFrom(mailFrom);
            }
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Sent {} html email to {}", logContext, email);
        } catch (Exception ex) {
            log.error("Failed to send {} html email to {}", logContext, email, ex);
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
        String latestRemark = findLatestDisplayRemark(claim);
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
        String latestRemark = findLatestDisplayRemark(claim);

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

        appendRemark(body, StringUtils.hasText(remark) ? remark : findLatestDisplayRemark(claim));

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
        String latestRemark = findLatestDisplayRemark(claim);

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
        String latestRemark = findLatestDisplayRemark(claim);

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

        appendRemark(body, StringUtils.hasText(remark) ? remark : findLatestDisplayRemark(claim));

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

    private String buildEmployeeAddedPendingApprovalBody(UserPersonalDetails employee, String hrUsername) {
        UserCompanyDetails companyDetails = employee.getUserCompanyDetails();
        CompanyTypes company = companyDetails.getCompanyTypes();
        StaffCategories staffCategory = companyDetails.getStaffCategories();

        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222;">
                    <p>Dear Admin team,</p>
                    <p>An Employee has been added by <strong>%s</strong> of <strong>%s</strong> and is pending your approval. The details are as follows:</p>
                    <p><strong>Employee details</strong></p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 700px;">
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold; width: 35%%;">Company</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Staff Category</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">EPF No</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Employee Name</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Married/Unmarried</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Date of Birth</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">NIC (Above 16 years)</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                    </table>
                    <p>Please login to the WeCare system to continue the approval process.<br/>
                    <a href="https://wecare-admin.dsi.lk/care-admin">https://wecare-admin.dsi.lk/care-admin</a></p>
                    <p>This is an automated notification. Please do not reply to this email.</p>
                    <p>Regards,<br/>WeCare system<br/>Automated Notification</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(safeValue(hrUsername)),
                escapeHtml(safeValue(company != null ? company.getDescription() : null)),
                escapeHtml(safeValue(company != null ? company.getDescription() : null)),
                escapeHtml(safeValue(staffCategory != null ? staffCategory.getDescription() : null)),
                escapeHtml(safeValue(employee.getEpfNo())),
                escapeHtml(getEmployeeName(employee)),
                escapeHtml(employee.getMaritalStatus() != null ? employee.getMaritalStatus().getDescription() : "-"),
                escapeHtml(formatDate(employee.getDob())),
                escapeHtml(safeValue(employee.getNic()))
        );
    }

    private String buildEmployeeDeactivatedBody(UserPersonalDetails employee, String hrUsername) {
        UserCompanyDetails companyDetails = employee.getUserCompanyDetails();
        CompanyTypes company = companyDetails.getCompanyTypes();
        StaffCategories staffCategory = companyDetails.getStaffCategories();

        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222;">
                    <p>Dear Admin team,</p>
                    <p>An Employee has been deactivated by <strong>%s</strong> of <strong>%s</strong>. The details are as follows:</p>
                    <p><strong>Employee details</strong></p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 700px;">
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold; width: 35%%;">Company</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Staff Category</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">EPF No</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Employee Name</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Terminated date</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                    </table>
                    <p>Please login to the WeCare system for more details.<br/>
                    <a href="https://wecare-admin.dsi.lk/care-admin">https://wecare-admin.dsi.lk/care-admin</a></p>
                    <p>This is an automated notification. Please do not reply to this email.</p>
                    <p>Regards,<br/>WeCare system<br/>Automated Notification</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(safeValue(hrUsername)),
                escapeHtml(safeValue(company != null ? company.getDescription() : null)),
                escapeHtml(safeValue(company != null ? company.getDescription() : null)),
                escapeHtml(safeValue(staffCategory != null ? staffCategory.getDescription() : null)),
                escapeHtml(safeValue(employee.getEpfNo())),
                escapeHtml(getEmployeeName(employee)),
                escapeHtml(formatDate(companyDetails.getTerminateDate()))
        );
    }

    private String buildDependentApprovedByHrBody(ClaimsDependents dependent, String hrUsername) {
        ApplicationUser applicationUser = dependent.getApplicationUser();
        UserPersonalDetails employee = applicationUser.getUserPersonalDetails();
        UserCompanyDetails companyDetails = employee.getUserCompanyDetails();
        CompanyTypes company = companyDetails.getCompanyTypes();
        StaffCategories staffCategory = companyDetails.getStaffCategories();
        String companyDescription = safeValue(company != null ? company.getDescription() : null);

        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222;">
                    <p>Dear Admin team,</p>
                    <p>A dependent has been approved by <strong>%s</strong> of <strong>%s</strong> for following employee. The details are as follows:</p>
                    <p><strong>Employee details</strong></p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 700px; margin-bottom: 16px;">
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold; width: 35%%;">Employee name</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Company</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">EPF number</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Staff category</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                    </table>
                    <p><strong>Dependent details</strong></p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 700px; margin-bottom: 16px;">
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold; width: 35%%;">Relationship</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Dependent name</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Date of Birth</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">NIC (Above 16 years)</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                    </table>
                    <p>Approved By: <strong>%s</strong> - <strong>%s</strong></p>
                    <p>Please login to the WeCare system for more details.<br/>
                    <a href="https://wecare-admin.dsi.lk/care-admin">https://wecare-admin.dsi.lk/care-admin</a></p>
                    <p>This is an automated notification. Please do not reply to this email.</p>
                    <p>Regards,<br/>WeCare system<br/>Automated Notification</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(safeValue(hrUsername)),
                escapeHtml(companyDescription),
                escapeHtml(getEmployeeName(employee)),
                escapeHtml(companyDescription),
                escapeHtml(safeValue(employee.getEpfNo())),
                escapeHtml(safeValue(staffCategory != null ? staffCategory.getDescription() : null)),
                escapeHtml(dependent.getRelationCategory() != null ? dependent.getRelationCategory().getDescription() : "-"),
                escapeHtml(getDependentName(dependent)),
                escapeHtml(formatDate(dependent.getDob())),
                escapeHtml(safeValue(dependent.getNic())),
                escapeHtml(safeValue(hrUsername)),
                escapeHtml(companyDescription)
        );
    }

    private String buildCivilStatusApprovedByHrBody(com.dtech.admin.model.MaritalStatus civilStatusUpdate, String hrUsername) {
        ApplicationUser applicationUser = civilStatusUpdate.getApplicationUser();
        UserPersonalDetails employee = applicationUser.getUserPersonalDetails();
        UserCompanyDetails companyDetails = employee.getUserCompanyDetails();
        CompanyTypes company = companyDetails != null ? companyDetails.getCompanyTypes() : null;
        StaffCategories staffCategory = companyDetails != null ? companyDetails.getStaffCategories() : null;
        String companyDescription = safeValue(company != null ? company.getDescription() : null);

        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222;">
                    <p>Dear Admin team,</p>
                    <p>A marriage certificate has been added by the following employee and approved by <strong>%s</strong> of <strong>%s</strong>. The details are as follows:</p>
                    <p><strong>Employee details</strong></p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 700px; margin-bottom: 16px;">
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold; width: 35%%;">Employee name</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Company</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">EPF number</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Staff category</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                    </table>
                    <p><strong>Dependent details</strong></p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 700px; margin-bottom: 16px;">
                        <tr>
                            <th style="border: 1px solid #d9d9d9; padding: 8px; text-align: left;">Relationship</th>
                            <th style="border: 1px solid #d9d9d9; padding: 8px; text-align: left;">Dependent name</th>
                            <th style="border: 1px solid #d9d9d9; padding: 8px; text-align: left;">Date of Birth</th>
                            <th style="border: 1px solid #d9d9d9; padding: 8px; text-align: left;">NIC (Above 16 years)</th>
                        </tr>
                        %s
                    </table>
                    <p>Please login to the WeCare system for more details.<br/>
                    <a href="https://wecare-admin.dsi.lk/care-admin">https://wecare-admin.dsi.lk/care-admin</a></p>
                    <p>This is an automated notification. Please do not reply to this email.</p>
                    <p>Regards,<br/>WeCare system<br/>Automated Notification</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(safeValue(hrUsername)),
                escapeHtml(companyDescription),
                escapeHtml(getEmployeeName(employee)),
                escapeHtml(companyDescription),
                escapeHtml(safeValue(employee.getEpfNo())),
                escapeHtml(safeValue(staffCategory != null ? staffCategory.getDescription() : null)),
                buildCivilStatusDependentRows(applicationUser)
        );
    }

    private String buildStaffCategoryTransferredBody(UserPersonalDetails employee,
                                                     StaffCategories previousStaffCategory,
                                                     StaffCategories newStaffCategory,
                                                     Date effectiveDate,
                                                     String hrUsername) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222;">
                    <p>Dear Admin team,</p>
                    <p>The following employee promoted as <strong>%s</strong>. Promoted details as follows;</p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 700px;">
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold; width: 35%%;">EPF</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Name</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">NIC</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Promoted Staff Category</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Previous Staff Category</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Effective date</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                    </table>
                    <p>Please login to the WeCare system for more details.<br/>
                    <a href="https://wecare-admin.dsi.lk/care-admin">https://wecare-admin.dsi.lk/care-admin</a></p>
                    <p>This is an automated notification. Please do not reply to this email.</p>
                    <p>Regards,<br/>WeCare system<br/>Automated Notification</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(safeValue(newStaffCategory != null ? newStaffCategory.getDescription() : null)),
                escapeHtml(safeValue(employee.getEpfNo())),
                escapeHtml(getEmployeeName(employee)),
                escapeHtml(safeValue(employee.getNic())),
                escapeHtml(safeValue(newStaffCategory != null ? newStaffCategory.getDescription() : null)),
                escapeHtml(safeValue(previousStaffCategory != null ? previousStaffCategory.getDescription() : null)),
                escapeHtml(formatDate(effectiveDate))
        );
    }

    private String buildCivilStatusDependentRows(ApplicationUser applicationUser) {
        if (applicationUser == null || CollectionUtils.isEmpty(applicationUser.getClaimsDependents())) {
            return """
                    <tr>
                        <td colspan="4" style="border: 1px solid #d9d9d9; padding: 8px;">No approved spouse or in-law dependents available.</td>
                    </tr>
                    """;
        }

        List<ClaimsDependents> dependents = applicationUser.getClaimsDependents().stream()
                .filter(Objects::nonNull)
                .filter(dependent -> Workflow.APPROVED.equals(dependent.getStatus()))
                .filter(dependent -> dependent.getRelationCategory() != null)
                .filter(dependent -> isCivilStatusDependent(dependent.getRelationCategory()))
                .sorted(Comparator
                        .comparing((ClaimsDependents dependent) -> dependent.getRelationCategory().getDescription())
                        .thenComparing(this::getDependentName))
                .toList();

        if (dependents.isEmpty()) {
            return """
                    <tr>
                        <td colspan="4" style="border: 1px solid #d9d9d9; padding: 8px;">No approved spouse or in-law dependents available.</td>
                    </tr>
                    """;
        }

        StringBuilder rows = new StringBuilder();
        for (ClaimsDependents dependent : dependents) {
            rows.append("""
                    <tr>
                        <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(dependent.getRelationCategory().getDescription()),
                    escapeHtml(getDependentName(dependent)),
                    escapeHtml(formatDate(dependent.getDob())),
                    escapeHtml(safeValue(dependent.getNic()))
            ));
        }
        return rows.toString();
    }

    private boolean isCivilStatusDependent(RelationCategory relationCategory) {
        return RelationCategory.WIFE.equals(relationCategory)
                || RelationCategory.HUSBAND.equals(relationCategory)
                || RelationCategory.FATHER_IN_LAW.equals(relationCategory)
                || RelationCategory.MOTHER_IN_LAW.equals(relationCategory);
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

    private String findLatestDisplayRemark(InsuranceClaimsRequest claim) {
        String remark = ApprovalRemarkUtil.resolveLevelTwoOrThreeRemark(claim);
        return StringUtils.hasText(remark) ? remark : "";
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

    private String getEmployeeName(UserPersonalDetails employee) {
        if (employee == null) {
            return "N/A";
        }
        String firstName = StringUtils.hasText(employee.getFirstName()) ? employee.getFirstName().trim() : "";
        String lastName = StringUtils.hasText(employee.getLastName()) ? employee.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return StringUtils.hasText(fullName) ? fullName : "N/A";
    }

    private String getDependentName(ClaimsDependents dependent) {
        if (dependent == null) {
            return "N/A";
        }
        String firstName = StringUtils.hasText(dependent.getFirstName()) ? dependent.getFirstName().trim() : "";
        String lastName = StringUtils.hasText(dependent.getLastName()) ? dependent.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return StringUtils.hasText(fullName) ? fullName : "N/A";
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "-";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String safeValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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

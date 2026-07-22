package com.dtech.admin.util;

import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.ApprovalWorkflowRejectReason;
import com.dtech.admin.model.InsuranceClaimsRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ApprovalRemarkUtil {

    private static final Set<ApprovalLevel> DISPLAY_LEVELS = EnumSet.of(
            ApprovalLevel.LEVEL02,
            ApprovalLevel.LEVEL03
    );

    private ApprovalRemarkUtil() {
    }

    public static String resolveLevelTwoOrThreeRemark(InsuranceClaimsRequest claim) {
        if (claim == null) {
            return null;
        }
        return resolveLevelTwoOrThreeRemark(claim.getApprovalWorkFlows());
    }

    public static String resolveLevelTwoOrThreeRemark(List<ApprovalWorkFlow> workflows) {
        if (CollectionUtils.isEmpty(workflows)) {
            return null;
        }

        return workflows.stream()
                .filter(workflow -> workflow != null && DISPLAY_LEVELS.contains(workflow.getApprovalLevel()))
                .filter(workflow -> StringUtils.hasText(resolveWorkflowRemark(workflow)))
                .max(Comparator.comparing(ApprovalWorkFlow::getApprovedDate, Comparator.nullsLast(Date::compareTo)))
                .map(ApprovalRemarkUtil::resolveWorkflowRemark)
                .map(String::trim)
                .orElse(null);
    }

    public static String resolveWorkflowRemark(ApprovalWorkFlow workflow) {
        if (workflow == null) {
            return null;
        }
        String reasonText = formatRejectReasons(workflow.getRejectReasons());
        return StringUtils.hasText(reasonText) ? reasonText : workflow.getRejectedRemark();
    }

    public static String formatRejectReasons(List<ApprovalWorkflowRejectReason> rejectReasons) {
        return formatRejectReasons(rejectReasons, false);
    }

    public static String formatRejectReasonsForNotification(List<ApprovalWorkflowRejectReason> rejectReasons) {
        return formatRejectReasons(rejectReasons, true);
    }

    private static String formatRejectReasons(List<ApprovalWorkflowRejectReason> rejectReasons,
                                              boolean omitZeroAmounts) {
        if (CollectionUtils.isEmpty(rejectReasons)) {
            return null;
        }
        String text = rejectReasons.stream()
                .filter(reason -> reason != null && reason.getAmount() != null)
                .map(reason -> formatRejectReason(reason, omitZeroAmounts))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
        return StringUtils.hasText(text) ? text : null;
    }

    public static BigDecimal sumRejectReasonAmounts(List<ApprovalWorkflowRejectReason> rejectReasons) {
        if (CollectionUtils.isEmpty(rejectReasons)) {
            return BigDecimal.ZERO;
        }
        return rejectReasons.stream()
                .filter(Objects::nonNull)
                .map(ApprovalWorkflowRejectReason::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String formatRejectReason(ApprovalWorkflowRejectReason reason, boolean omitZeroAmounts) {
        String description = StringUtils.hasText(reason.getReasonDescription())
                ? reason.getReasonDescription().trim()
                : reason.getReasonCode();
        if (StringUtils.hasText(reason.getRemark())) {
            description = description + " - " + reason.getRemark().trim();
        }
        if (omitZeroAmounts && reason.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return description;
        }
        String amount = reason.getAmount().stripTrailingZeros().toPlainString();
        return description + " - " + (omitZeroAmounts ? "Rs. " : "") + amount;
    }
}

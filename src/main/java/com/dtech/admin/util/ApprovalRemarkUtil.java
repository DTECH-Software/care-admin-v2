package com.dtech.admin.util;

import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.model.ApprovalWorkFlow;
import com.dtech.admin.model.InsuranceClaimsRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
                .filter(workflow -> StringUtils.hasText(workflow.getRejectedRemark()))
                .max(Comparator.comparing(ApprovalWorkFlow::getApprovedDate, Comparator.nullsLast(Date::compareTo)))
                .map(ApprovalWorkFlow::getRejectedRemark)
                .map(String::trim)
                .orElse(null);
    }
}

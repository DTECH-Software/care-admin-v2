package com.dtech.admin.util;

import com.dtech.admin.model.ApprovalWorkflowRejectReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApprovalRemarkUtilTest {

    @Test
    void notificationFormattingOmitsZeroAmountButKeepsReasonAndRemark() {
        ApprovalWorkflowRejectReason nonZeroReason = reason(
                "No prescription", "test remark", new BigDecimal("500"));
        ApprovalWorkflowRejectReason zeroReason = reason(
                "Limit exceed", "zero remark", BigDecimal.ZERO);

        String result = ApprovalRemarkUtil.formatRejectReasonsForNotification(
                List.of(nonZeroReason, zeroReason));

        assertEquals(
                "No prescription - test remark - Rs. 500, Limit exceed - zero remark",
                result);
    }

    @Test
    void standardFormattingStillIncludesZeroAmount() {
        ApprovalWorkflowRejectReason zeroReason = reason(
                "Limit exceed", "zero remark", BigDecimal.ZERO);

        assertEquals(
                "Limit exceed - zero remark - 0",
                ApprovalRemarkUtil.formatRejectReasons(List.of(zeroReason)));
    }

    private ApprovalWorkflowRejectReason reason(String description, String remark, BigDecimal amount) {
        ApprovalWorkflowRejectReason reason = new ApprovalWorkflowRejectReason();
        reason.setReasonCode(description);
        reason.setReasonDescription(description);
        reason.setRemark(remark);
        reason.setAmount(amount);
        return reason;
    }
}

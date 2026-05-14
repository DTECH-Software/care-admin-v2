package com.dtech.admin.dto.response;

import lombok.Data;

@Data
public class DailyTaskMedicalRowDTO {
    private String staffType;
    private String date;
    private long claimsReceived;
    private String claimsReceivedDetails;
    private long notYetProcessed;
    private String notYetProcessedDetails;
    private DailyTaskReportStageDTO firstCheckComplete;
    private long pendingRequirementClaims;
    private DailyTaskReportStageDTO haveToPreparePaymentAttachments;
    private DailyTaskReportStageDTO preparePaymentAttachments;
    private DailyTaskReportStageDTO haveToHandoverForFinalCheck;
    private DailyTaskReportStageDTO handoverForFinalCheck;
    private DailyTaskReportStageDTO haveToCompleteFinalCheck;
    private DailyTaskReportStageDTO finalCheckComplete;
    private DailyTaskReportStageDTO haveToInputToCurrentSystem;
    private DailyTaskReportStageDTO inputToCurrentSystem;
    private DailyTaskReportStageDTO haveToPaymentsComplete;
    private DailyTaskReportStageDTO paymentsCompleted;
    private String otherWorks;
}

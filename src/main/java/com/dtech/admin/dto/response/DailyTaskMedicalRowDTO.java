package com.dtech.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class DailyTaskMedicalRowDTO {
    private String staffType;
    private String date;
    @JsonIgnore
    private long claimsReceived;
    private String claimsReceivedDetails;
    @JsonIgnore
    private long notYetProcessed;
    private String notYetProcessedDetails;
    private DailyTaskReportStageDTO firstCheckComplete;
    @JsonIgnore
    private long pendingRequirementClaims;
    private DailyTaskReportStageDTO haveToPreparePaymentAttachments;
    private DailyTaskReportStageDTO preparePaymentAttachments;
    @JsonIgnore
    private DailyTaskReportStageDTO haveToHandoverForFinalCheck;
    @JsonIgnore
    private DailyTaskReportStageDTO handoverForFinalCheck;
    private DailyTaskReportStageDTO haveToCompleteFinalCheck;
    private DailyTaskReportStageDTO finalCheckComplete;
    @JsonIgnore
    private DailyTaskReportStageDTO haveToInputToCurrentSystem;
    @JsonIgnore
    private DailyTaskReportStageDTO inputToCurrentSystem;
    private DailyTaskReportStageDTO haveToPaymentsComplete;
    private DailyTaskReportStageDTO paymentsCompleted;
    private String otherWorks;
}

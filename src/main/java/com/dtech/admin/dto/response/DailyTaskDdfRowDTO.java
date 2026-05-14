package com.dtech.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class DailyTaskDdfRowDTO {
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
    @JsonIgnore
    private DailyTaskReportStageDTO haveToHandoverToAuthorizedPerson;
    private DailyTaskReportStageDTO handoverToAuthorizedPerson;
    @JsonIgnore
    private DailyTaskReportStageDTO haveToHandoverToFinalCheck;
    private DailyTaskReportStageDTO handoverToFinalCheck;
    private DailyTaskReportStageDTO haveToCompleteFinalCheck;
    private DailyTaskReportStageDTO finalCheckComplete;
    private DailyTaskReportStageDTO haveToPreparePayment;
    @JsonIgnore
    private DailyTaskReportStageDTO haveToCheckedPaymentAdviceAndFundTransfer;
    @JsonIgnore
    private DailyTaskReportStageDTO paymentAdviceAndFundTransferChecked;
    @JsonIgnore
    private long returnedClaims;
    private DailyTaskReportStageDTO haveToPaymentsCompleted;
    private DailyTaskReportStageDTO paymentsCompleted;
    private String otherWorks;
}

package com.dtech.admin.dto.response;

import lombok.Data;

@Data
public class DailyTaskDdfRowDTO {
    private String staffType;
    private String date;
    private long claimsReceived;
    private long notYetProcessed;
    private DailyTaskReportStageDTO firstCheckComplete;
    private long pendingRequirementClaims;
    private DailyTaskReportStageDTO haveToHandoverToAuthorizedPerson;
    private DailyTaskReportStageDTO handoverToAuthorizedPerson;
    private DailyTaskReportStageDTO haveToHandoverToFinalCheck;
    private DailyTaskReportStageDTO handoverToFinalCheck;
    private DailyTaskReportStageDTO haveToCompleteFinalCheck;
    private DailyTaskReportStageDTO finalCheckComplete;
    private DailyTaskReportStageDTO haveToPreparePayment;
    private DailyTaskReportStageDTO haveToCheckedPaymentAdviceAndFundTransfer;
    private DailyTaskReportStageDTO paymentAdviceAndFundTransferChecked;
    private long returnedClaims;
    private DailyTaskReportStageDTO haveToPaymentsCompleted;
    private DailyTaskReportStageDTO paymentsCompleted;
    private String otherWorks;
}

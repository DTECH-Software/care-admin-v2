package com.dtech.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedClaimTotalReportNormalStaffDTO {
    private String staffCategory;
    private String claimReceivedPeriod;
    private long receivedClaims;
    private long stillProcessingClaims;
    private long settledClaims;
    private long rejectedClaims;
    private long assumeRejectClaims;
    private long notYetProcessedClaims;
    private long wecareSettledClaims;
}

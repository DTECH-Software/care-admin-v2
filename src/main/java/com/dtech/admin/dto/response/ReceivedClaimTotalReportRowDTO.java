package com.dtech.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedClaimTotalReportRowDTO {
    private String staffCategory;
    private String claimReceivedPeriod;
    private long receivedClaims;
    private long settledClaims;
    private String remark;
}

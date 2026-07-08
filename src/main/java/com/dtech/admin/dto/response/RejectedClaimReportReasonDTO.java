package com.dtech.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectedClaimReportReasonDTO {
    private long rejectedClaims;
    private String returnReason;
    private BigDecimal rejectedAmount;

    public RejectedClaimReportReasonDTO(long rejectedClaims, String returnReason) {
        this.rejectedClaims = rejectedClaims;
        this.returnReason = returnReason;
        this.rejectedAmount = BigDecimal.ZERO;
    }
}

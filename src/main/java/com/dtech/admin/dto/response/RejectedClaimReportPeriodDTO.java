package com.dtech.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectedClaimReportPeriodDTO {
    private Long periodId;
    private String periodDescription;
    private long totalReceivedClaims;
    private long totalRejectedClaims;
    private BigDecimal rejectedPercentage;
    private List<RejectedClaimReportCompanyDTO> companies;
}

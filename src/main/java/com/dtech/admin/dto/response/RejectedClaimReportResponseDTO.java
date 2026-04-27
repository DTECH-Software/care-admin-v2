package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RejectedClaimReportResponseDTO {
    private String title;
    private String subTitle;
    private String staffCategoryTitle;
    private String period;
    private String monthTitle;
    private long totalReceivedClaims;
    private long totalRejectedClaims;
    private BigDecimal rejectedPercentage;
    private List<RejectedClaimReportCompanyDTO> companies;
}

package com.dtech.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectedClaimReportCompanyDTO {
    private String companyCode;
    private String companyDescription;
    private long receivedClaims;
    private long rejectedClaims;
    private List<RejectedClaimReportReasonDTO> reasons;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ReceivedClaimTotalReportResponseDTO {
    private String period;
    private String monthTitle;
    private ReceivedClaimTotalReportNormalStaffDTO normalStaffClaims;
    private List<ReceivedClaimTotalReportRowDTO> thirdPartyClaims;
    private List<ReceivedClaimTotalReportRowDTO> wecareClaims;
    private List<ReceivedClaimTotalReportRowDTO> ddfClaims;
}

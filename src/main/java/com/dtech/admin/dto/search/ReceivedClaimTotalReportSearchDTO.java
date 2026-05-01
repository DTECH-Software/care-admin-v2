package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class ReceivedClaimTotalReportSearchDTO {
    @JsonAlias({"dateFrom"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String fromDate;

    @JsonAlias({"dateTo"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String toDate;

    @JsonAlias({"normalstaffassumeRejectClaims", "normalStaffAssumedRejectClaims", "assumeRejectClaims"})
    private Long normalStaffAssumeRejectClaims;
}

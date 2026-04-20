package com.dtech.admin.dto.search;

import lombok.Data;

@Data
public class ThirdPartyIndoorClaimBatchSearchDTO {
    private String batchNo;
    private String fileName;
    private String status;
    private String uploadedBy;
    private String fromDate;
    private String toDate;
}


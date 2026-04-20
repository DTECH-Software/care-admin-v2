package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ThirdPartyIndoorClaimBatchResponseDTO {
    private Long id;
    private String batchNo;
    private String fileName;
    private String fileType;
    private String status;
    private String statusDescription;
    private Integer totalRows;
    private Integer validRows;
    private Integer invalidRows;
    private Integer duplicateRows;
    private Integer importedRows;
    private Date createdDate;
    private String createdBy;
    private List<ThirdPartyIndoorClaimBatchRowResponseDTO> rows;
}


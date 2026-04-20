package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ThirdPartyIndoorClaimValidationResponseDTO {
    private String fileName;
    private Integer totalRows;
    private Integer validRows;
    private Integer invalidRows;
    private Integer duplicateRows;
    private List<ThirdPartyIndoorClaimValidationRowResponseDTO> rows;
}


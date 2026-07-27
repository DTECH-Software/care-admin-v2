package com.dtech.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemarkReferenceDTO {
    private String code;
    private String description;
    private boolean includeInRejectedClaimReport;
}

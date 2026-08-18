package com.dtech.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeePreviousEmploymentItemResponseDTO {
    private String companyCode;
    private String companyDescription;
    private String epfNo;
}

package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class EmployeePreviousEmploymentResponseDTO {
    private String nic;
    private List<EmployeePreviousEmploymentItemResponseDTO> previousEmployment;
}

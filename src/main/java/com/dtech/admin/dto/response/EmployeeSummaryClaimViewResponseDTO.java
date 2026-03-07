package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class EmployeeSummaryClaimViewResponseDTO {
    private EmployeeSummaryClaimInfoDTO claim;
    private List<EmployeeSummaryApprovalHistoryDTO> approvalHistory;
    private List<EmployeeSummaryBalanceRowDTO> balances;
}

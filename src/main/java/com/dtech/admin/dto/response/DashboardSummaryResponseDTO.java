package com.dtech.admin.dto.response;

import lombok.Data;

@Data
public class DashboardSummaryResponseDTO {
    private EmployeeSummary employee;
    private ClaimSummary healthClaims;
    private ClaimSummary deathClaims;

    @Data
    public static class EmployeeSummary {
        private long totalEmployees;
        private long dependentsTotal;
        private long approvedDependents;
        private long rejectedDependents;
        private long pendingDependents;
    }

    @Data
    public static class ClaimSummary {
        private long total;
        private long approved;
        private long rejected;
        private long underReview;
    }
}

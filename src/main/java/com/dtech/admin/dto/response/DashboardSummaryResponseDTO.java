package com.dtech.admin.dto.response;

import lombok.Data;

@Data
public class DashboardSummaryResponseDTO {
    private EmployeeSummary employee;
    private java.util.List<CompanySummary> companies;
    private java.util.List<StaffCategorySummary> staffCategories;
    private java.util.List<PolicySummary> policies;
    private ClaimSummary healthClaims;
    private ClaimSummary deathClaims;

    @Data
    public static class EmployeeSummary {
        private long totalEmployees;
        private long totalMaleEmployees;
        private long totalFemaleEmployees;
        private long dependentsTotal;
        private long totalMaleDependents;
        private long totalFemaleDependents;
        private long approvedDependents;
        private long rejectedDependents;
        private long pendingDependents;
    }

    @Data
    public static class ClaimSummary {
        private long total;
        private long todayTotal;
        private long approved;
        private long rejected;
        private long underReview;
    }

    @Data
    public static class CompanySummary {
        private String code;
        private String description;
        private long totalEmployees;
    }

    @Data
    public static class StaffCategorySummary {
        private String code;
        private String description;
        private long totalEmployees;
    }

    @Data
    public static class PolicySummary {
        private String code;
        private String description;
    }
}

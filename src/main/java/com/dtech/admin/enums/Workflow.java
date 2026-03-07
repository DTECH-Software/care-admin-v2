package com.dtech.admin.enums;

public enum Workflow {

    UNDER_REVIEW("Under Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    ACTIVE("Active");

    private final String description;

    Workflow(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
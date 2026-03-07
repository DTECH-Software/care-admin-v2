package com.dtech.admin.enums;

public enum ApprovalLevel implements DescribableEnum{

    LEVEL01("L1"),
    LEVEL02("L2"),
    LEVEL03("L3");

    private final String description;

    ApprovalLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

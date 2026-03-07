package com.dtech.admin.enums;

public enum AuthorizerStatus implements DescribableEnum {

    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    UP_REJECTED("Uploader Rejected");

    private final String description;

    AuthorizerStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

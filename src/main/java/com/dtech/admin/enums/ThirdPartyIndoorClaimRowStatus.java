package com.dtech.admin.enums;

public enum ThirdPartyIndoorClaimRowStatus implements DescribableEnum {
    IMPORTED("Imported"),
    DUPLICATE("Duplicate"),
    FAILED("Failed");

    private final String description;

    ThirdPartyIndoorClaimRowStatus(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}


package com.dtech.admin.enums;

public enum ThirdPartyIndoorClaimBatchStatus implements DescribableEnum {
    IMPORTED("Imported"),
    PARTIAL_IMPORTED("Partially Imported"),
    FAILED("Failed");

    private final String description;

    ThirdPartyIndoorClaimBatchStatus(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}


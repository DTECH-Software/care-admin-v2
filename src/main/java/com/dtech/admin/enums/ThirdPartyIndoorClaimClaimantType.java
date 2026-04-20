package com.dtech.admin.enums;

public enum ThirdPartyIndoorClaimClaimantType implements DescribableEnum {
    EMPLOYEE("Employee"),
    DEPENDENT("Dependent");

    private final String description;

    ThirdPartyIndoorClaimClaimantType(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}


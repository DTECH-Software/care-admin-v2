package com.dtech.admin.enums;

public enum DeathClaimDocTypes implements DescribableEnum {

    OTHER_CERTIFICATE("Other certificate"),
    DEATH_CERTIFICATE("Death certificate");

    private final String description;
    DeathClaimDocTypes(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
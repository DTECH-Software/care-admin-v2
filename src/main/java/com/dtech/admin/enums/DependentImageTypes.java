package com.dtech.admin.enums;

public enum DependentImageTypes implements DescribableEnum{
    BIRTH("Birth"),
    MARRIED("Married");

    private final String description;
    DependentImageTypes(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}

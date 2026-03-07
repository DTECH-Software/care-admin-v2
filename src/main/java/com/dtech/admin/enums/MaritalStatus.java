package com.dtech.admin.enums;

public enum MaritalStatus implements DescribableEnum {

    MARRIED("Married"),
    UNMARRIED("Unmarried"),
    DIVORCE("Divorce");

    private final String description;

    MaritalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
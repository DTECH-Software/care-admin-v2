package com.dtech.admin.enums;

public enum Gender implements DescribableEnum{
    MALE("Male"),
    FEMALE("Female");

    private final String description;

    Gender(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
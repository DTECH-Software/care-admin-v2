package com.dtech.admin.enums;

public enum PaymentType implements DescribableEnum{

    FULL("Full"),
    HALF("Half");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}

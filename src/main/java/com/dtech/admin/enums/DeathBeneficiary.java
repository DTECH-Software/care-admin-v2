package com.dtech.admin.enums;

public enum DeathBeneficiary implements DescribableEnum {

    EMPLOYEE("Employee"),
    MOTHER("Mother"),
    FATHER("Father"),
    CHILD("Children"),
    WIFE("Wife"),
    HUSBAND("Husband"),
    FATHER_IN_LAW("Father in law"),
    MOTHER_IN_LAW("Mother in law"),
    BROTHER("Brother"),
    SISTER("Sister");

    private final String description;

    DeathBeneficiary(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

package com.dtech.admin.enums;

public enum Month implements DescribableEnum {
    JANUARY("January"),
    FEBRUARY("February"),
    MARCH("March"),
    APRIL("April"),
    MAY("May"),
    JUNE("June"),
    JULY("July"),
    AUGUST("August"),
    SEPTEMBER("September"),
    OCTOBER("October"),
    NOVEMBER("November"),
    DECEMBER("December"),
    ADJUSTMENT("Adjustment");

    private final String description;

    Month(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

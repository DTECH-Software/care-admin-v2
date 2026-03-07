package com.dtech.admin.enums;

public enum TreatmentType {

    INDOOR("Indoor"),
    OUTDOOR("Outpatient"),
    CRIC("Critical illness"),
    DEATH("Death Donation Funds");

    private final String description;

    TreatmentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

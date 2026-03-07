package com.dtech.admin.enums;

public enum AuditTask implements DescribableEnum {
    SYSTEM_USER_NOT_FOUND("The username you entered does not exist"),
    USER_INACTIVE_OR_FORCE_TO_CHANGE_PASSWORD("Your account is inactive or requires a password reset"),
    USER_LOGIN_TO_EXPIRED_PASSWORD_PERIOD_FORCE_TO_CHANGE_PASSWORD("Your password has expired"),
    USER_ATTEMPT_LIMIT_EXCEED("You have exceeded the maximum number of login attempts"),
    USER_ENTERED_PASSWORD_MISMATCH("The password you entered is incorrect"),
    USER_LOGIN_SUCCESS("Login successful"),

    //Section
    ALL_SECTIONS_LOADED_SUCCESS("All sections loaded successfully"),

    //User privilege management
    GETTING_ALL_REFERENCE_DATA("Getting all the reference data "),
    ASSIGNED_UNASSIGNED_TASK_RETRIEVED("Getting all the assigned and un assigned tasks"),
    TASK_ASSIGNED_SUCCESS("Task assigned successfully"),
    SEARCH_FILTER("Filter search results"),
    VIEW_DATA("View data success"),
    UPDATE_DATA("Update data success"),
    DELETE_DATA("Delete data success"),
    ADD_DATA("Add data success"),

    //Password reset
    PASSWORD_RESET_SUCCESS("Password reset success"),
    PASSWORD_RESET_OTP_SENT_SUCCESS("Password reset OTP sent success");


    private final String description;
    AuditTask(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}

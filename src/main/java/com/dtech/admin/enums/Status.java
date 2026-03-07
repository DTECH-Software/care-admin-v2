/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 9:53 AM
 * <p>
 */

package com.dtech.admin.enums;

public enum Status implements DescribableEnum {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    DELETE("Delete");

    private final String description;

    Status(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

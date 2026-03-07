/**
 * User: Himal_J
 * Date: 5/1/2025
 * Time: 8:37 AM
 * <p>
 */

package com.dtech.admin.dto.audit;

import lombok.Data;

@Data
public class CommonAuditDTO {
    private String code;
    private String description;
    private String statusDescription;

    @Override
    public String toString() {
        return "Code: " + code + ", Description: " + description + ", Status: " + statusDescription;
    }
}

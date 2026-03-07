/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 10:11 AM
 * <p>
 */

package com.dtech.admin.dto.audit;

import lombok.Data;

@Data
public class WebUserRolePageTaskAuditDTO {

    private String role;
    private String page;
    private String task;

    @Override
    public String toString() {
        return "User role: " + role + ", Page: " + page + ", Task: " + task;
    }

}

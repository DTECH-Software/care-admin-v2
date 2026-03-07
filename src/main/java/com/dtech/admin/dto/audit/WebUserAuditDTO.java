/**
 * User: Himal_J
 * Date: 4/30/2025
 * Time: 2:13 PM
 * <p>
 */

package com.dtech.admin.dto.audit;

import lombok.Data;

@Data
public class WebUserAuditDTO {
    public String username;
    public String userRole;
    public String userRoleDescription;
    public String nic;
    public String status;
    public String statusDescription;
    public String email;
    public String mobile;
    public String firstName;
    public String lastName;

    public String toString() {
        return "Username: " + username +
                ", User role: " + userRoleDescription +
                ", Status: " + statusDescription +
                ", NIC: " + nic +
                ", Email: " + email +
                ", Mobile: " + mobile +
                ", First name: " + firstName +
                ", Last name: " + lastName;
    }
}

/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 2:18 PM
 * <p>
 */

package com.dtech.admin.dto.search;

import lombok.Data;

@Data
public class SystemUserSearchDTO {
    private String newUsername;
    private String userRole;
    private String nic;
    private String email;
    private String mobile;
    private String firstName;
    private String lastName;
    private String status;
    private String loginStatus;
}

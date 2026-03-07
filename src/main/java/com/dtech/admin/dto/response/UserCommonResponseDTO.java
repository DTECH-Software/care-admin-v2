/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 2:45 PM
 * <p>
 */

package com.dtech.admin.dto.response;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.enums.ApprovalLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserCommonResponseDTO extends CommonAuditResponseDTO{
    private Long id;
    private String username;
    private String newUsername; // for system user page
    private String email;
    private String mobile;
    private String status;
    private String statusDescription;
    private String loginStatus;
    private String loginStatusDescription;
    private SimpleBaseDTO userRole;
    private String firstName;
    private String lastName;
    private String nic;
    private SimpleBaseDTO company;
    private Boolean isApprovalLevel;
    private String approvalLevel;
    private String approvalLevelDescription;
    private Boolean isDeathApprovalLevel;
    private String deathApprovalLevel;
    private String deathApprovalLevelDescription;
//    private SimpleBaseDTO companyGroup;
}

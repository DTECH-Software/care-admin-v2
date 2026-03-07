/**
 * User: Himal_J
 * Date: 4/30/2025
 * Time: 2:52 PM
 * <p>
 */

package com.dtech.admin.dto.request.validator;

import com.dtech.admin.enums.Status;
import com.dtech.admin.validator.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Conditional(selected = "isApprovalLevel",
        values = {
                "true",
        }, required = {"approvalLevel"}, message = "Approval level is required",groups = {OnUpdate.class,OnAdd.class})

@Conditional(selected = "isDeathApprovalLevel",
        values = {
                "true",
        }, required = {"deathApprovalLevel"}, message = "Death approval level is required",groups = {OnUpdate.class,OnAdd.class})
public class SystemUserRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotNull(message = "ID is required",groups = {OnGet.class,OnUpdate.class, OnDelete.class})
    private Long id;
    @NotBlank(message = "Username is required",groups = {OnAdd.class,OnUpdate.class,OnOTP.class})
    private String newUsername;
    @NotBlank(message = "User role is required",groups = {OnAdd.class,OnUpdate.class})
    private String userRole;
    @NotBlank(message = "NIC is required",groups = {OnAdd.class,OnUpdate.class})
    @Pattern(regexp = "^[0-9]{9}[Vv]?$|^[0-9]{12}$",
            message = "Invalid NIC number. It must be 9 digits optionally followed by 'V' or 'v', or exactly 12 digits",
            groups = {OnAdd.class,OnUpdate.class})
    private String nic;
    @NotBlank(message = "Email is required",groups = {OnAdd.class,OnUpdate.class})
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Please enter a valid email address",
            groups = {OnAdd.class,OnUpdate.class})
    private String email;
    @NotBlank(message = "Mobile is required",groups = {OnAdd.class,OnUpdate.class})
    @Pattern(regexp = "^(071|074|070|077|075|078|072|076)[0-9]{7}$",
            message = "Invalid mobile number. It must start with 071, 074, 070, 077, 075, 078, 072, or 076, and be followed by 7 digits",
            groups = {OnAdd.class,OnUpdate.class})
    private String mobile;
    @NotBlank(message = "First name is required",groups = {OnAdd.class,OnUpdate.class})
    private String firstName;
    @NotBlank(message = "Last name is required",groups = {OnAdd.class,OnUpdate.class})
    private String lastName;
    @NotBlank(message = "Status is required",groups = {OnUpdate.class})
    @ValidEnum(enumClass = Status.class, message = "Invalid status",groups = {OnUpdate.class})
    private String status;
    @NotNull(message = "Approval level status is required",groups = {OnAdd.class,OnUpdate.class})
    private Boolean isApprovalLevel;
    private String approvalLevel;
    @NotNull(message = "Death level status is required",groups = {OnAdd.class,OnUpdate.class})
    private Boolean isDeathApprovalLevel;
    private String deathApprovalLevel;

}

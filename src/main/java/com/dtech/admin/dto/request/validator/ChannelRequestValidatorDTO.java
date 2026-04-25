/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 12:23 PM
 * <p>
 */

package com.dtech.admin.dto.request.validator;


import com.dtech.admin.enums.Messages;
import com.dtech.admin.validator.Conditional;
import com.dtech.admin.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Conditional(selected = "message",
        values = {
                "LOGIN",
                "DASHBOARD",
                "GET_ASSIGNED_UN_ASSIGNED_TASK",
                "REF_DATA",
                "FILTER_LIST",
                "VIEW",
                "UPDATE",
                "DELETE",
                "ADD",
                "RESET_PASSWORD",
                "FILE_DOWNLOAD",
                "LOGOUT"
        }, required = {"username"}, message = "Username is required.")
public class ChannelRequestValidatorDTO {
    @NotBlank(message = "IP is required.")
    private String ip;
    @NotBlank(message = "Message is required.")
    @ValidEnum(enumClass = Messages.class, message = "Invalid message.")
    private String message;
    private String username;
    @NotBlank(message = "User agent is required")
    private String userAgent;
}

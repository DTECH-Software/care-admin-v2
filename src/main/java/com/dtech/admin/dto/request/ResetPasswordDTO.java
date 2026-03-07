package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResetPasswordDTO extends ChannelRequestDTO {
    private String password;
    private String confirmPassword;

    public String toString() {
        return ", Password: " + password;
    }
}

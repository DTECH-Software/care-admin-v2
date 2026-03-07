/**
 * User: Himal_J
 * Date: 4/24/2025
 * Time: 8:35 AM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoginRequestDTO extends ChannelRequestDTO {
    private String password;

    public String toString() {
        return "Username: " + getUsername() +
                ", Password: " + password;
    }
}
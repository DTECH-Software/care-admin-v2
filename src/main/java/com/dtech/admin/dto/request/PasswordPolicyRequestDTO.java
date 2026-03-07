/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 8:29 AM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PasswordPolicyRequestDTO extends ChannelRequestDTO{
    private Long id;
    private int minUpperCase;
    private int minLowerCase;
    private int minNumbers;
    private int minSpecialCharacters;
    private int minLength;
    private int maxLength;
    private int passwordHistory;
    private int attemptExceedCount;
    private int otpExceedCount;
}

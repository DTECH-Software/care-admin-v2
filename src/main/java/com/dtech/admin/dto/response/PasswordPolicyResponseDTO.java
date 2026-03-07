/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 9:35 AM
 * <p>
 */

package com.dtech.admin.dto.response;

import lombok.Data;

@Data
public class PasswordPolicyResponseDTO {
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

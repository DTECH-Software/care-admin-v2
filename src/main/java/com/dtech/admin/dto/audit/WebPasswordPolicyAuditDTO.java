/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 9:35 AM
 * <p>
 */

package com.dtech.admin.dto.audit;

import lombok.Data;

@Data
public class WebPasswordPolicyAuditDTO {

    private int minUpperCase;
    private int minLowerCase;
    private int minNumbers;
    private int minSpecialCharacters;
    private int minLength;
    private int maxLength;
    private int passwordHistory;
    private int attemptExceedCount;
    private int otpExceedCount;

    @Override
    public String toString() {
        return "Min upper case: " + minUpperCase + ", Min lower case: " + minLowerCase
                + ", Min numbers: " + minNumbers + ", Min special characters: " + minSpecialCharacters + ", Min length: " + minLength
                + ", Max length: " + maxLength + ", Password history: " + passwordHistory + ", Attempt exceed count: " + attemptExceedCount + ", OTP exceed count: " + otpExceedCount;
    }
}

/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 12:03 PM
 * <p>
 */

package com.dtech.admin.dto.response;


import lombok.Data;

@Data
public class UsernamePolicyResponseDTO {
    private Long id;
    private int minUpperCase;
    private int minLowerCase;
    private int minNumbers;
    private int minSpecialCharacters;
    private int minLength;
    private int maxLength;
}

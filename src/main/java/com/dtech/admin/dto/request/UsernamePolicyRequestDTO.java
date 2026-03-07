/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 10:30 AM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UsernamePolicyRequestDTO extends ChannelRequestDTO {
    private Long id;
    private int minUpperCase;
    private int minLowerCase;
    private int minNumbers;
    private int minSpecialCharacters;
    private int minLength;
    private int maxLength;
}

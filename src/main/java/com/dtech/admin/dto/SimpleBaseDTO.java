/**
 * User: Himal_J
 * Date: 2/22/2025
 * Time: 6:32 PM
 * <p>
 */

package com.dtech.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleBaseDTO {
    private String code;
    private String description;
}

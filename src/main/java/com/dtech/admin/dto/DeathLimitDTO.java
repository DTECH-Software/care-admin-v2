/**
 * User: Himal_J
 * Date: 3/25/2025
 * Time: 1:12 PM
 * <p>
 */

package com.dtech.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DeathLimitDTO {
    private String dependentId;
    private BigDecimal deathLimit;
    private String ageRange;
}

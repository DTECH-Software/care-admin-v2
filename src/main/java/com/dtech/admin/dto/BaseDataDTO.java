/**
 * User: Himal_J
 * Date: 4/4/2025
 * Time: 4:22 PM
 * <p>
 */

package com.dtech.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseDataDTO {

    private String description;
    private BigDecimal amount = BigDecimal.ZERO;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<BaseDataDTO> sheetDetails;

    BaseDataDTO(String description, BigDecimal amount) {
        this.description = description;
        this.amount = amount;
    }
}

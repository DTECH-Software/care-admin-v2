/**
 * User: Himal_J
 * Date: 4/5/2025
 * Time: 6:06 PM
 * <p>
 */

package com.dtech.admin.dto.response;

import com.dtech.admin.dto.BaseDataDTO;
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
public class ExpensesResponseDTO {
    private String sheetCode;
    private String sheetDescription;
    List<BaseDataDTO> sheetDetails;
    private BigDecimal sumOfSheet;

}

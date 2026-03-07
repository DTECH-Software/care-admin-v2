/**
 * User: Himal_J
 * Date: 4/7/2025
 * Time: 2:09 PM
 * <p>
 */

package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExpensesDTO {
    private String title;
    List<ExpensesResponseDTO> baseExpenses = new ArrayList<>();
    private BigDecimal sumOfExpenses = BigDecimal.ZERO;
}

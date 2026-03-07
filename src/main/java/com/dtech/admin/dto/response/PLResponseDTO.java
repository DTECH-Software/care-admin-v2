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
public class PLResponseDTO {
    private String sheetCategoryCode;
    private String sheetCategoryDescription;
    List<BaseDataDTO> sheetCategoryDetails;
    private BigDecimal sumOfSheetCategory;
}

package com.dtech.admin.dto;

import com.dtech.admin.dto.response.ExpensesResponseDTO;
import lombok.Data;

import java.util.List;

@Data
public class PnlDto {
    private String companyCode;
    private String companyDescription;
    private String monthCode;
    private String monthDescription;
    private String yearCode;
    private String yearDescription;
    private List<ExpensesResponseDTO> sheetsList;
    private Pnl pnl;

}
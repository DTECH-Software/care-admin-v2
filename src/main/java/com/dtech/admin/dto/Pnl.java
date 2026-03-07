package com.dtech.admin.dto;

import com.dtech.admin.dto.response.PLResponseDTO;
import lombok.Data;

import java.util.List;

@Data
public class Pnl {
    private double earningAfterTax;
    private double earningBeforeTax;
    private double grossProfit;
    private List<PLResponseDTO> sheetSummaryDetails;
}
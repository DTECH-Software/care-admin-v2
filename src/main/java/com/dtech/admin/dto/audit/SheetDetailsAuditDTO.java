package com.dtech.admin.dto.audit;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SheetDetailsAuditDTO {
    private String sheet;
    private String sheetDescription;
    private String description;
    private BigDecimal amount;

    public String toString() {
        return "Sheet: " + sheet +
                ", Description: " + description +
                ", Amount: " + amount ;
    }
}

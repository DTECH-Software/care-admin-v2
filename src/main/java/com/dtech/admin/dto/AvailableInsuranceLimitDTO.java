package com.dtech.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailableInsuranceLimitDTO {
    private BigDecimal availableLimit;
    private BigDecimal fundLimit;
}
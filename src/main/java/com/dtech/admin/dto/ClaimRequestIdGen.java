package com.dtech.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ClaimRequestIdGen {
    private String staffCategory;
    private String year;
    private String company;
}

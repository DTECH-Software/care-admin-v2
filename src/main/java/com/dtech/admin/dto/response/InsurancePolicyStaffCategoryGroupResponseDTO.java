package com.dtech.admin.dto.response;

import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class InsurancePolicyStaffCategoryGroupResponseDTO {
    private SimpleBaseDTO insurancePolicy;
    private List<StaffCategoryMapping> staffCategories;

    public InsurancePolicyStaffCategoryGroupResponseDTO(SimpleBaseDTO insurancePolicy) {
        this.insurancePolicy = insurancePolicy;
        this.staffCategories = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    public static class StaffCategoryMapping {
        private String code;
        private String description;
        private String mainCategoryCode;
        private String mainCategoryDescription;
    }
}

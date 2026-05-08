package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class ClaimStatusReportSearchDTO {
    @JsonAlias({"dateFrom"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String fromDate;

    @JsonAlias({"dateTo"})
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String toDate;

    @JsonAlias({"companyCode"})
    private String company;
    @JsonAlias({"staffCategoryCode"})
    private String staffCategory;
    private String epfNo;
    private String employeeName;
    private String dependentName;
    private String dependentCategory;
    @JsonAlias({"treatmentCode"})
    private String treatment;
    private String treatmentCategory;
    @JsonAlias({"claimStatusCode", "status"})
    private String claimStatus;

    @JsonProperty("treatmentCategory")
    @JsonAlias({"treatmentCategoryCode"})
    public void setTreatmentCategory(Object treatmentCategory) {
        this.treatmentCategory = extractFilterValue(treatmentCategory);
    }

    private String extractFilterValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof Map<?, ?> map) {
            Object code = map.get("code");
            if (code != null) {
                return String.valueOf(code);
            }
            Object valueField = map.get("value");
            if (valueField != null) {
                return String.valueOf(valueField);
            }
            Object id = map.get("id");
            if (id != null) {
                return String.valueOf(id);
            }
            Object description = map.get("description");
            if (description != null) {
                return String.valueOf(description);
            }
        }
        return String.valueOf(value);
    }
}

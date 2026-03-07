package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class TreatmentCategoryCompanyReportSearchDTO {
    private String company;
    private String staffCategory;
    private String treatment;
    private String treatmentCategory;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String fromDate;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String toDate;
}

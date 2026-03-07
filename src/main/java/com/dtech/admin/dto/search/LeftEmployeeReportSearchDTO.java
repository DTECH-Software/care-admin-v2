package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
public class LeftEmployeeReportSearchDTO {
    private String company;
    private String staffCategory;
    private String facility;
    private String terminateDateFrom;
    private String terminateDateTo;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> status;
}

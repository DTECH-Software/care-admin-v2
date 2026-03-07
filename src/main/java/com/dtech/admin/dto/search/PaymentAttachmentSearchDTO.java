package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
public class PaymentAttachmentSearchDTO {
    private String attachmentNo;
    private String company;
    private String staffCategory;
    private String treatmentCategory;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateFrom;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateTo;
    @JsonAlias({"statusList"})
    private List<String> status;
}

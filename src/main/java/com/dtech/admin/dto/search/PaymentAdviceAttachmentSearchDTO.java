package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class PaymentAdviceAttachmentSearchDTO {
    private String attachmentNo;
    private String company;
    private String staffCategory;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateFrom;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private String dateTo;
    @JsonIgnore
    private List<String> staffCategoryCodes;
}

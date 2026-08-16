package com.dtech.admin.dto.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class SupportTicketSearchDTO {
    private String ticketNo;
    private String companyCode;
    private String category;
    private String subject;
    private String priority;
    private String status;
    private String createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fromDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date toDate;
}

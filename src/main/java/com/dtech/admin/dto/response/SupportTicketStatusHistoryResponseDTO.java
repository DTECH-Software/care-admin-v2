package com.dtech.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class SupportTicketStatusHistoryResponseDTO {
    private String oldStatus;
    private String newStatus;
    private String remark;
    private String changedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date changedDate;
}

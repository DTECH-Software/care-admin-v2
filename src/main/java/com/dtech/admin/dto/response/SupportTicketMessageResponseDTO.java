package com.dtech.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class SupportTicketMessageResponseDTO {
    private Long id;
    private String authorUsername;
    private String authorName;
    private String message;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdDate;
    private List<SupportTicketAttachmentResponseDTO> attachments;
}

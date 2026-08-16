package com.dtech.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class SupportTicketResponseDTO {
    private Long id;
    private String ticketNo;
    private String systemType;
    private String systemDescription;
    private String companyCode;
    private String companyDescription;
    private String category;
    private String subject;
    private String description;
    private String priority;
    private String status;
    private String resolution;
    private String createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastModifiedDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date resolvedDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closedDate;
    private Integer replyCount;
    private Integer attachmentCount;
    private List<SupportTicketMessageResponseDTO> replies;
    private List<SupportTicketAttachmentResponseDTO> attachments;
    private List<SupportTicketStatusHistoryResponseDTO> statusHistory;
}

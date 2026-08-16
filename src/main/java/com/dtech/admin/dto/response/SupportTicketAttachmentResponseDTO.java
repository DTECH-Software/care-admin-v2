package com.dtech.admin.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupportTicketAttachmentResponseDTO {
    private Long id;
    private Long messageId;
    private Long documentId;
    private String fileName;
    private String fileType;
    private String file;
}

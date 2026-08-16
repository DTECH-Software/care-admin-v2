package com.dtech.admin.dto.request;

import com.dtech.admin.validator.ValidFileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportTicketAttachmentRequestDTO {
    @NotBlank(message = "Attachment file name is required.")
    @Size(max = 255, message = "Attachment file name cannot exceed 255 characters.")
    private String fileName;

    @NotBlank(message = "Attachment file type is required.")
    @ValidFileType(message = "Only PNG, JPEG, JPG, and PDF attachments are allowed.")
    private String fileType;

    @NotBlank(message = "Attachment content is required.")
    private String file;
}

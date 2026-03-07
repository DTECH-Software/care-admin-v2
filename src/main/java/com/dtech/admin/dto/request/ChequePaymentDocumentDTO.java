package com.dtech.admin.dto.request;

import com.dtech.admin.validator.ValidFileType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChequePaymentDocumentDTO {
    private String type;
    private String file;
    @NotBlank(message = "File type is required.")
    @ValidFileType(message = "Only PNG, JPEG, JPG, and PDF file types are allowed.")
    private String fileType;
    @NotBlank(message = "File name is required.")
    private String fileName;
}

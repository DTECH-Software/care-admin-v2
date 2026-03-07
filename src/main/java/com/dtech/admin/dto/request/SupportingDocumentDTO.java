package com.dtech.admin.dto.request;

import com.dtech.admin.enums.DependentImageTypes;
import com.dtech.admin.enums.ImgType;
import com.dtech.admin.validator.ValidEnum;
import com.dtech.admin.validator.ValidFileType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportingDocumentDTO {
    @NotBlank(message = "File type is required.")
    @ValidEnum(enumClass = ImgType.class, message = "Invalid file type.")
    private String type;
    @NotBlank(message = "File is required.")
    private String file;
    @NotBlank(message = "File type is required.")
    @ValidFileType(message = "Only PNG, JPEG, JPG, and PDF file types are allowed.")
    private String fileType;
    @NotBlank(message = "File name is required.")
    private String fileName;
}
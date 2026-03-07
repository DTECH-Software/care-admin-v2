package com.dtech.admin.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileDocumentDTO {
    private String type;
    private MultipartFile file;
    private String fileType;
    private String fileName;
}

package com.dtech.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonPropertyOrder({"type", "fileName", "fileType", "doc"})
@Data
public class ChequePaymentDocumentResponseDTO {
    private String type;
    private String doc;
    private String fileName;
    private String fileType;
}

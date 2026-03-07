/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 12:46 PM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;

@Data
public class DocumentUploadRequestDTO {
    private String type;
    private byte[] document;
    private String fileName;
    private String fileType;
}

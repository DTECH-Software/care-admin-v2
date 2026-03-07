/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 3:40 PM
 * <p>
 */

package com.dtech.admin.dto.response;


import com.dtech.admin.enums.DocType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDownloadResponseDTO {
    private String type;
    private String fileName;
    private String fileType;
    private String doc;
}

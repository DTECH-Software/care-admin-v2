/**
 * User: Himal_J
 * Date: 4/4/2025
 * Time: 9:10 AM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.multipart.MultipartFile;
@EqualsAndHashCode(callSuper = true)
@Data
public class FileUploadRequestDTO extends ChannelRequestDTO{
    private MultipartFile file;
}

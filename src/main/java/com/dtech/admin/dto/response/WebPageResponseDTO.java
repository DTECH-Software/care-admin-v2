/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 4:07 PM
 * <p>
 */

package com.dtech.admin.dto.response;

import com.dtech.admin.enums.Status;
import lombok.Data;

@Data
public class WebPageResponseDTO {
    private String code;
    private String url;
    private String description;
    private Status status;
}

/**
 * User: Himal_J
 * Date: 5/5/2025
 * Time: 7:00 AM
 * <p>
 */

package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;
@Data
public class CommonAuditResponseDTO {
    private Date createdDate;
    private Date lastModifiedDate;
    private String createdBy;
    private String lastModifiedBy;
}

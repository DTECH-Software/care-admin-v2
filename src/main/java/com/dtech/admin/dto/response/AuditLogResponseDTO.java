package com.dtech.admin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class AuditLogResponseDTO {
    private Long id;
    private Date dateTime;
    private String source;
    private String pageCode;
    private String pageDescription;
    private String taskCode;
    private String taskDescription;
    private String username;
    private String ipAddress;
    private String userAgent;
    private String oldValue;
    private String newValue;
}

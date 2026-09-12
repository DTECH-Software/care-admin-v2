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
    private String module;
    private String action;
    private String result;
    private Integer responseStatus;
    private String requestPath;
    private String httpMethod;
    private Long durationMs;
    private String correlationId;
    private String clientAppVersion;
    private String clientPlatform;
    private String appUpdateStatus;
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

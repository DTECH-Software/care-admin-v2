package com.dtech.admin.dto.search;

import lombok.Data;

import java.util.Date;

@Data
public class AuditLogSearchDTO {
    private String source;
    private String module;
    private String action;
    private String result;
    private String pageCode;
    private String taskCode;
    private String username;
    private String ipAddress;
    private String clientAppVersion;
    private String clientPlatform;
    private String appUpdateStatus;
    private Date fromDate;
    private Date toDate;
}

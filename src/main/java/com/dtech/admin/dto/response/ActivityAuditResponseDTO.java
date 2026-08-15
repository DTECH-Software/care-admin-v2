package com.dtech.admin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ActivityAuditResponseDTO {
    private Long id;
    private Date dateTime;
    private String activity;
    private String module;
    private String moduleDescription;
    private String performedBy;
    private String result;
    private String ipAddress;
    private String device;
    private String correlationId;
}

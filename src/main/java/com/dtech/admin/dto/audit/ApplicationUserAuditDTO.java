package com.dtech.admin.dto.audit;

import lombok.Data;

@Data
public class ApplicationUserAuditDTO {
    private String username;
    private UserPersonalDetailsAuditDTO userPersonalDetails;
}

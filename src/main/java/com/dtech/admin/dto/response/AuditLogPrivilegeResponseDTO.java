package com.dtech.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuditLogPrivilegeResponseDTO {
    private boolean search;
    private boolean view;
}

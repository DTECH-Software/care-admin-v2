package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeManagementRequestDTO extends ChannelRequestDTO{
    private Long id;
    private String staffCategory;
    private String policy;
    private String loginStatus;
    private String userStatus;
    private Date effectiveDate;
    private SupportingDocumentDTO documents;
}

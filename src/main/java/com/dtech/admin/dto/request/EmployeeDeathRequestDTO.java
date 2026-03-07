package com.dtech.admin.dto.request;

import com.dtech.admin.dto.SupportingDocumentDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeDeathRequestDTO extends ChannelRequestDTO{
    private String remark;
    private Long id;
    private Date deathDate;
    private List<SupportingDocumentDTO> documents;
}

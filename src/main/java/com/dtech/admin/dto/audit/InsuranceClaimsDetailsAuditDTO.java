package com.dtech.admin.dto.audit;

import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.Data;

import java.util.Date;

@Data
public class InsuranceClaimsDetailsAuditDTO {
    private TreatmentAuditDTO treatment;
    private SimpleBaseDTO treatmentCategory;
    private Date fromTreatmentDate;
    private Date toTreatmentDate;
    private String disease;
}

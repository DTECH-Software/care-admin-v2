package com.dtech.admin.dto.audit;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.model.*;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
public class CompanyDetailsAuditDTO  {
    private SimpleBaseDTO companyTypes;
    private SimpleBaseDTO staffCategories;
    private SimpleBaseDTO staffTypes;
    private String designation;
    private Date permanentDate;
    private Date terminateDate;
    private SimpleBaseDTO insurancePolicy;
}

package com.dtech.admin.dto.request;

import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.Data;

import java.util.Date;

@Data
public class UserCompanyDetailsRequestDTO {
    private String companyTypeCode;
    private String paymentCompanyCode;
    private SimpleBaseDTO paymentCompany;
    private String deathPaymentCompanyCode;
    private SimpleBaseDTO deathPaymentCompany;
    private String staffCategoryCode;
    private String staffTypeCode;
    private String designation;
    private Date permanentDate;
    private Date terminateDate;
    private String insurancePolicyCode;
    private SimpleBaseDTO insurancePolicy;
    private String facility;
}

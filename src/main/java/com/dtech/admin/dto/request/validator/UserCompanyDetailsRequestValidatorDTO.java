package com.dtech.admin.dto.request.validator;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.enums.Facility;
import com.dtech.admin.validator.Conditional;
import com.dtech.admin.validator.OnAdd;
import com.dtech.admin.validator.OnUpdate;
import com.dtech.admin.validator.ValidEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class UserCompanyDetailsRequestValidatorDTO {
    @NotBlank(message = "Company type is required",groups = {OnAdd.class})
    private String companyTypeCode;
    private String paymentCompanyCode;
    @Valid
    private SimpleBaseDTO paymentCompany;
    private String deathPaymentCompanyCode;
    @Valid
    private SimpleBaseDTO deathPaymentCompany;
    @NotBlank(message = "Staff category is required",groups = {OnAdd.class})
    private String staffCategoryCode;
    @NotBlank(message = "Staff type is required",groups = {OnAdd.class})
    private String staffTypeCode;
    @NotBlank(message = "Designation is required",groups = {OnAdd.class, OnUpdate.class})
    private String designation;
    @NotNull(message = "Permanent date is required",groups = {OnAdd.class})
    private Date permanentDate;
   // @NotNull(message = "Terminate date is required",groups = {OnUpdate.class})
    private Date terminateDate;
    private String insurancePolicyCode;
    @Valid
    private SimpleBaseDTO insurancePolicy;
    @ValidEnum(enumClass = Facility.class, message = "Facility is invalid.",groups = {OnAdd.class})
    private String facility;
}

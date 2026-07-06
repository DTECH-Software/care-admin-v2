package com.dtech.admin.dto.request.validator;

import com.dtech.admin.dto.request.SupportingDocumentDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.validator.OnGet;
import com.dtech.admin.validator.OnStaffCategoryUpdate;
import com.dtech.admin.validator.OnUpdate;
import com.dtech.admin.validator.ValidEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeManagementRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotNull(message = "ID is required",groups = {OnGet.class, OnUpdate.class, OnStaffCategoryUpdate.class})
    private Long id;
    @NotBlank(message = "Staff category is required",groups = {OnStaffCategoryUpdate.class})
    private String staffCategory;
    @NotBlank(message = "Policy is required",groups = {OnStaffCategoryUpdate.class})
    private String policy;
    @ValidEnum(enumClass = Status.class, message = "Login status is invalid",groups = {OnUpdate.class})
    private String loginStatus;
    @ValidEnum(enumClass = Status.class, message = "User status is invalid",groups = {OnUpdate.class})
    private String userStatus;
    @NotNull(message = "Effective date is required",groups = {OnStaffCategoryUpdate.class})
    private Date effectiveDate;
    @NotNull(message = "Supporting document is required.",groups = {OnStaffCategoryUpdate.class})
    @Valid
    private SupportingDocumentDTO documents;
}

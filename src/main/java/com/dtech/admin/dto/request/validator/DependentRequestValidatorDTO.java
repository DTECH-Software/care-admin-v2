package com.dtech.admin.dto.request.validator;

import com.dtech.admin.enums.Workflow;
import com.dtech.admin.enums.DependentCategory;
import com.dtech.admin.enums.Facility;
import com.dtech.admin.enums.Gender;
import com.dtech.admin.enums.RelationCategory;
import com.dtech.admin.validator.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Conditional(selected = "status",
        values = {
                "REJECTED",
        }, required = {"remark"}, message = "Remark is required",groups = {OnUpdate.class})
public class DependentRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "ID is required",groups = {OnGet.class, OnUpdate.class, OnDependentDetailsUpdate.class})
    private Long id;
    @NotBlank(message = "Status is required",groups = {OnUpdate.class})
    @ValidEnum(enumClass = Workflow.class, message = "Status is invalid",groups = {OnUpdate.class})
    private String status;
    private String remark;
    @ValidEnum(enumClass = DependentCategory.class, message = "Dependent category is invalid",groups = {OnDependentDetailsUpdate.class})
    private String dependentCategory;
    private String initials;
    private String firstName;
    private String lastName;
    private Date dob;
    @ValidEnum(enumClass = Gender.class, message = "Gender is invalid",groups = {OnDependentDetailsUpdate.class})
    private String gender;
    private String nic;
    private String jobTitle;
    @ValidEnum(enumClass = Facility.class, message = "Eligible facility is invalid",groups = {OnDependentDetailsUpdate.class})
    private String eligibleFacility;
    @ValidEnum(enumClass = RelationCategory.class, message = "Relation category is invalid",groups = {OnDependentDetailsUpdate.class})
    private String relationCategory;
    private Boolean liveStatus;
}

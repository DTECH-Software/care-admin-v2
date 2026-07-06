package com.dtech.admin.dto.request.validator;


import com.dtech.admin.enums.Gender;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.Title;
import com.dtech.admin.validator.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper=true)
public class EmployeeDetailsRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotNull(message = "ID is required",groups = {OnGet.class, OnUpdate.class, OnDelete.class})
    private Long id;
    @NotBlank(message = "EPF is required",groups = {OnAdd.class})
    private String epfNo;
    @NotBlank(message = "Initials is required",groups = {OnAdd.class,OnUpdate.class})
    private String initials;
    @NotBlank(message = "Title is required",groups = {OnAdd.class,OnUpdate.class})
    @ValidEnum(enumClass = Title.class, message = "Title is invalid",groups = {OnAdd.class,OnUpdate.class})
    private String title;
    @NotBlank(message = "Gender is required",groups = {OnAdd.class})
    @ValidEnum(enumClass = Gender.class, message = "Gender is invalid",groups = {OnAdd.class})
    private String gender;
    @NotBlank(message = "First name is required",groups = {OnAdd.class,OnUpdate.class})
    private String firstName;
    @NotBlank(message = "Last name is required",groups = {OnAdd.class,OnUpdate.class})
    private String lastName;
    @NotBlank(message = "NIC is required",groups = {OnAdd.class,OnUpdate.class})
    @Pattern(regexp = "^[0-9]{9}[Vv]?$|^[0-9]{12}$", message = "Invalid NIC number. It must be 9 digits optionally followed by 'V' or 'v', or exactly 12 digits",groups = {OnAdd.class})
    private String nic;
    @NotBlank(message = "Email is required",groups = {OnAdd.class,OnUpdate.class})
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please enter a valid email address",groups = {OnAdd.class,OnUpdate.class})
    private String email;
    private String mobileNo;
    private Boolean noMobileNumber;
    @NotNull(message = "Marital status is required",groups = {OnAdd.class,OnUpdate.class})
    private String maritalStatus;
    @NotNull(message = "DOB is required",groups = {OnAdd.class})
    private Date dob;
    @NotNull(message = "User address is required",groups = {OnAdd.class,OnUpdate.class})
    @Valid
    private EmployeeAddressRequestValidatorDTO userAddress;
    @NotNull(message = "Company is required",groups = {OnAdd.class,OnUpdate.class})
    @Valid
    private UserCompanyDetailsRequestValidatorDTO userCompanyDetails;
    @NotBlank(message = "User status is required",groups = {OnAdd.class,OnUpdate.class})
    @ValidEnum(enumClass = Status.class, message = "Status is invalid",groups = {OnAdd.class,OnUpdate.class})
    private String userStatus;
}

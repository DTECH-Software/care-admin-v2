package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnAdd;
import com.dtech.admin.validator.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeAddressRequestValidatorDTO {
    @NotBlank(message = "Street number is required",groups = {OnAdd.class, OnUpdate.class})
    private String streetNo;
    @NotBlank(message = "Street one is required",groups = {OnAdd.class,OnUpdate.class})
    private String street1;
    private String street2;
    @NotBlank(message = "City is required",groups = {OnAdd.class,OnUpdate.class})
    private String city;
}

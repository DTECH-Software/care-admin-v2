package com.dtech.admin.dto.search;

import lombok.Data;

@Data
public class EmployeeSearchDTO {
    private String epfNo;
    private String firstName;
    private String lastName;
    private String nic;
    private String email;
    private String mobileNo;
    private String userStatus;
    private String companyCode;
    private String staffCategoryCode;
    private String insurancePolicyCode;
    private String username;
}

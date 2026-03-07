package com.dtech.admin.dto.search;

import lombok.Data;

@Data
public class ClaimDependentSearchDTO {
    private String dependentCategory;
    private String firstName;
    private String lastName;
    private String nic;
    private String relationCategory;
    private String status;
    private Boolean liveStatus;
    private String employeeNic;
    private String company;
    private String epfNo;
    private String staffCategory;
    private String dependentName;
}

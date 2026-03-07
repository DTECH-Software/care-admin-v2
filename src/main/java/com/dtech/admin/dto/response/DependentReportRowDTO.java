package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class DependentReportRowDTO {
    private Long dependentId;
    private String dependentCategory;
    private String dependentCategoryDescription;
    private String relationCategory;
    private String relationCategoryDescription;
    private String initials;
    private String firstName;
    private String lastName;
    private String gender;
    private String genderDescription;
    private Date dob;
    private int age;
    private String nic;
    private String jobTitle;
    private String eligibleFacility;
    private String eligibleFacilityDescription;
    private String status;
    private String statusDescription;
    private Boolean liveStatus;
    private Date approvedDate;
    private String approvedUser;
    private String remark;
    private Long employeeId;
    private String epf;
    private String employeeName;
    private String companyCode;
    private String companyDescription;
    private String staffCategoryCode;
    private String staffCategoryDescription;
}

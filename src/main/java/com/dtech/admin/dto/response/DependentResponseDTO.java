package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class DependentResponseDTO {
    private String dependentCategory;
    private String dependentCategoryDescription;
    private String initials;
    private String firstName;
    private String lastName;
    private Date dob;
    private String gender;
    private String genderDescription;
    private String nic;
    private String jobTitle;
    private String eligibleFacility;
    private String eligibleFacilityDescription;
    private String relationCategory;
    private String relationCategoryDescription;
    private Boolean liveStatus;
    private Date createdDate;
    private int age;
}

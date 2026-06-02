package com.dtech.admin.dto.response;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class DependentDetailsResponseDTO extends CommonAuditResponseDTO {
    private Long id;
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
    private String status;
    private String statusDescription;
    private int age;
    private ApplicationUserResponseDTO applicationUser;
    private Boolean liveStatus;
    private List<DocumentDownloadResponseDTO> attachment;
    private Date approvedDate;
    private String approvedUser;
    private String remark;
}

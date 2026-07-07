package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class DependentRequestDTO extends ChannelRequestDTO {
    private Long id;
    private String status;
    private String remark;
    private String dependentCategory;
    private String initials;
    private String firstName;
    private String lastName;
    private Date dob;
    private String gender;
    private String nic;
    private String jobTitle;
    private String eligibleFacility;
    private String relationCategory;
    private Boolean liveStatus;
}

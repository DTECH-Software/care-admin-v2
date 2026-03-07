package com.dtech.admin.dto.request;

import com.dtech.admin.dto.EmployeeAddressDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeDetailsRequestDTO extends ChannelRequestDTO {
    private Long id;
    private String epfNo;
    private String initials;
    private String title;
    private String gender;
    private String firstName;
    private String lastName;
    private String nic;
    private String email;
    private String mobileNo;
    private String maritalStatus;
    private Date dob;
    private EmployeeAddressDTO userAddress;
    private UserCompanyDetailsRequestDTO userCompanyDetails;
    private String userStatus;
    private String staffCategory;
}

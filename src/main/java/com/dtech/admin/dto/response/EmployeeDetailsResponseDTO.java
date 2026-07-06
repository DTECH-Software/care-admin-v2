package com.dtech.admin.dto.response;

import com.dtech.admin.dto.EmployeeAddressDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeDetailsResponseDTO extends CommonAuditResponseDTO {
    private Long id;
    private String epfNo;
    private String initials;
    private String title;
    private String titleDescription;
    private String gender;
    private String genderDescription;
    private String firstName;
    private String lastName;
    private String nic;
    private String email;
    private String mobileNo;
    private Boolean noMobileNumber;
    private String maritalStatus;
    private String maritalStatusDescription;
    private Date dob;
    private EmployeeAddressDTO userAddress;
    private UserCompanyDetailsResponseDTO userCompanyDetails;
    private String userStatus;
    private String userStatusDescription;
    private int age;
    private EmployeeRejoinDetailsResponseDTO rejoinDetails;
}

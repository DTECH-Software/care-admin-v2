package com.dtech.admin.dto.audit;

import com.dtech.admin.enums.Gender;
import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.Title;
import lombok.Data;

import java.util.Date;
@Data
public class UserPersonalDetailsAuditDTO {
    private String epfNo;
    private Title title;
    private String titleDescription;
    private String initials;
    private String firstName;
    private String lastName;
    private String nic;
    private String email;
    private String mobileNo;
    private Gender gender;
    private String genderDescription;
    private String maritalStatus;
    private Date dob;
    private UserAddressAuditDTO userAddress;
    private CompanyDetailsAuditDTO userCompanyDetails;
    private Status userStatus;
    private String userStatusDescription;
}

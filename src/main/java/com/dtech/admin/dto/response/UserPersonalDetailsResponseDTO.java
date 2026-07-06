package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UserPersonalDetailsResponseDTO {
    private String initials;
    private String firstName;
    private String lastName;
    private String nic;
    /*new field*/
    private Long id;
    private String epfNo;
    private String title;
    private String titleDescription;
    private String email;
    private String mobileNo;
    private Boolean noMobileNumber;
    private String gender;
    private String genderDescription;
    private String maritalStatus;
    private String maritalStatusDescription;
    private Date dob;
    private int age;
    private UserAddressResponseDTO userAddress;
    private UserCompanyDetailsResponseDTO userCompanyDetails;
    private String userStatus;
    private String userStatusDescription;
    private Boolean isTemp;
    private String tempId;
    private DocumentDownloadResponseDTO birthImg;
    private DocumentDownloadResponseDTO maritalStatusDocument;
    private DocumentDownloadResponseDTO promoDoc;
}

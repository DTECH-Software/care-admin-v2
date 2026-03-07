package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class ApplicationUserResponseDTO {
    private Long id;
    private String username;
    private String primaryEmail;
    private String primaryMobile;
    private String loginStatus;
    private String loginStatusDescription;
    private boolean isReset;
    private Date lastPasswordChangeDate;
    private Date lastLoggedDate;
    private Date mbLastLoggedDate;
    private Date opLastLoggedDate;
    private boolean expectingFirstTimeLogging;
    private boolean expectingDependentsRegister;
    private Date passwordExpiredDate;
    private int attemptCount;
    private int otpAttemptCount;
    private Date otpAttemptResetTime;
    private UserPersonalDetailsResponseDTO userPersonalDetails;
    private Date createdDate;
    private String gender;
    private String genderDescription;
    private int age;
}

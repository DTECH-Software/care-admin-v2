package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ThirdPartyIndoorClaimBatchRowResponseDTO {
    private Long id;
    private Integer rowNo;
    private String externalReferenceNo;
    private String companyCode;
    private String epfNo;
    private String employeeName;
    private Integer policyYear;
    private String policyNo;
    private Date fromDate;
    private Date toDate;
    private Date intimatedDate;
    private Date paidDate;
    private BigDecimal nonPayableAmount;
    private String nonPayableItem;
    private BigDecimal claimAmount;
    private BigDecimal approvedAmount;
    private String remark;
    private String status;
    private String statusDescription;
    private String errorMessage;
    private Long insuranceClaimId;
    private String insuranceClaimRequestId;
}


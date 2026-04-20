package com.dtech.admin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ThirdPartyIndoorClaimBatchRowResponseDTO {
    private Long id;
    private Integer rowNo;
    private String externalReferenceNo;
    private String claimantType;
    private String claimantTypeDescription;
    private String epfNo;
    private String employeeNic;
    private String employeeName;
    private String dependentNic;
    private String dependentName;
    private String dependentRelation;
    private Date fromDate;
    private Date toDate;
    private String hospital;
    private String disease;
    private BigDecimal requestAmount;
    private BigDecimal approvedAmount;
    private String remark;
    private String status;
    private String statusDescription;
    private String errorMessage;
    private Long insuranceClaimId;
    private String insuranceClaimRequestId;
}


package com.dtech.admin.dto.response;

import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ClaimsRequestResponseDTO {
    private Long id;
    private String requestId;
    private BigDecimal requestAmount;
    private String requestStatus;
    private String requestStatusDescription;
    private String remark;
    private List<ApprovalWorkFlowResponseDTO> approvalWorkFlow;
    private InsuranceClaimsDetailsResponseDTO insuranceClaimsDetails;
    private DependentResponseDTO claimsDependents;
    private ApplicationUserResponseDTO employee;
    private String approvalLevel;
    private String approvalLevelDescription;
    private String paymentAdviceStatus;
    private String paymentAdviceStatusDescription;
    private String adviceNo;
    private String finalRemark;
    private Date finalApproveDate;
    private Date rejectionDate;
    private Map<String,Object> limits;
    private List<SimpleBaseDTO> policyList;
    private Date createdDate;
    private Date permanentDate;
    private Date promotionDate;
    private Date terminateDate;
    private List<SimpleBaseDTO> previousCompanies;
    private List<String> previousEpfs;
}

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClaimRequestDTO extends ChannelRequestDTO {
    private Long id;
    private String status;
    private String remark;
    private BigDecimal approvedAmount;
    private Long policyId;
    private BigDecimal availableLimit;
    private List<ApprovalRejectReasonDTO> rejectReasons;
}

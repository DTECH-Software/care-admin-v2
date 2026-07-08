package com.dtech.admin.dto.request.validator;

import com.dtech.admin.enums.Workflow;
import com.dtech.admin.validator.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
//@Conditional(selected = "status",
//        values = {
//                "REJECTED"
//        }, required = {"remark"}, message = "Remark is required ",groups = {OnUpdate.class})
@Conditional(selected = "status",
        values = {
                "APPROVED"
        }, required = {"approvedAmount"}, message = "Approved amount is required ",groups = {OnUpdate.class})
@Conditional(selected = "status",
        values = {
                "APPROVED"
        }, required = {"availableLimit"}, message = "Available limit is required ",groups = {OnUpdate.class})
@Conditional(selected = "status",
        values = {
                "APPROVED",
                "REJECTED"
        }, required = {"policyId"}, message = "Policy id is required ",groups = {OnUpdate.class})
public class ClaimRequestRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "ID is required",groups = {OnGet.class, OnUpdate.class, OnDeath.class})
    private Long id;
    private Long policyId;
    private BigDecimal availableLimit;
    @NotBlank(message = "Status is required",groups = {OnUpdate.class,OnDeath.class})
    @ValidEnum(enumClass = Workflow.class, message = "Status is invalid",groups = {OnUpdate.class,OnDeath.class})
    private String status;
    // @NotBlank(message = "Remark is required ",groups = {OnUpdate.class})
    private String remark;
    private BigDecimal approvedAmount;
    private List<ApprovalRejectReasonValidatorDTO> rejectReasons;

    @Data
    public static class ApprovalRejectReasonValidatorDTO {
        private String reasonCode;
        private String reasonDescription;
        private String reasonCategory;
        private BigDecimal amount;
        private String remark;
    }
}

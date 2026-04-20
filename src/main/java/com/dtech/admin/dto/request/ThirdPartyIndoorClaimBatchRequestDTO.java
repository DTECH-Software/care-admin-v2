package com.dtech.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ThirdPartyIndoorClaimBatchRequestDTO extends ChannelRequestDTO {

    @NotNull(message = "ID is required")
    private Long id;
}


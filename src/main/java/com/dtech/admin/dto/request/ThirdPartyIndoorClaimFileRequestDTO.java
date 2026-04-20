package com.dtech.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ThirdPartyIndoorClaimFileRequestDTO extends ChannelRequestDTO {

    @NotBlank(message = "File name is required")
    private String fileName;

    private String fileType;

    @NotBlank(message = "File is required")
    private String file;
}


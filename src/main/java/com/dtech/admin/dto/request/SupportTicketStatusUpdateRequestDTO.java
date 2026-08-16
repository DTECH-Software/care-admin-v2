package com.dtech.admin.dto.request;

import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SupportTicketStatusUpdateRequestDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Ticket ID is required.")
    private Long id;

    @NotBlank(message = "Status is required.")
    private String status;

    @Size(max = 1000, message = "Remark cannot exceed 1000 characters.")
    private String remark;

    @Size(max = 4000, message = "Resolution cannot exceed 4000 characters.")
    private String resolution;
}

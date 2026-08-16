package com.dtech.admin.dto.request;

import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SupportTicketViewRequestDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Ticket ID is required.")
    private Long id;
}

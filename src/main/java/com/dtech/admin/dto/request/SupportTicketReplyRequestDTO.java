package com.dtech.admin.dto.request;

import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SupportTicketReplyRequestDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Ticket ID is required.")
    private Long id;

    @NotBlank(message = "Reply is required.")
    @Size(max = 4000, message = "Reply cannot exceed 4000 characters.")
    private String reply;

    @Valid
    @Size(max = 5, message = "A reply can contain a maximum of 5 attachments.")
    private List<SupportTicketAttachmentRequestDTO> attachments = new ArrayList<>();
}

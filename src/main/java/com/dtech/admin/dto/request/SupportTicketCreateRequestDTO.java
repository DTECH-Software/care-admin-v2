package com.dtech.admin.dto.request;

import com.dtech.admin.dto.request.validator.ChannelRequestValidatorDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SupportTicketCreateRequestDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "Company code is required.")
    private String companyCode;

    @NotBlank(message = "Category is required.")
    @Size(max = 50, message = "Category cannot exceed 50 characters.")
    private String category;

    @NotBlank(message = "Subject is required.")
    @Size(max = 200, message = "Subject cannot exceed 200 characters.")
    private String subject;

    @NotBlank(message = "Description is required.")
    @Size(max = 4000, message = "Description cannot exceed 4000 characters.")
    private String description;

    @NotBlank(message = "Priority is required.")
    private String priority;

    @Valid
    @Size(max = 5, message = "A ticket can contain a maximum of 5 attachments.")
    private List<SupportTicketAttachmentRequestDTO> attachments = new ArrayList<>();
}

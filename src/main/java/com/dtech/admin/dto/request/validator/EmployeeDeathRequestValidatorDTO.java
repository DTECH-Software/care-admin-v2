package com.dtech.admin.dto.request.validator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeDeathRequestValidatorDTO extends ChannelRequestValidatorDTO {
    private String remark;
    @NotNull(message = "Employee is required.")
    private Long id;
    @NotNull(message = "Death date is required.")
    private Date deathDate;
    @NotNull(message = "Death certificate is required.")
    @NotEmpty(message = "Death certificate document is required.")
    @Valid
    private List<DeathSupportingDocumentValidatorDTO> documents;
}

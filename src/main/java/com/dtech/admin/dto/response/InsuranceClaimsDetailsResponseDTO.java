package com.dtech.admin.dto.response;

import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class InsuranceClaimsDetailsResponseDTO {
    private TreatmentResponseDTO treatment;
    private SimpleBaseDTO treatmentCategory;
    private Date fromTreatmentDate;
    private Date toTreatmentDate;
    private String disease;
    private List<DocumentDownloadResponseDTO> documents;
}

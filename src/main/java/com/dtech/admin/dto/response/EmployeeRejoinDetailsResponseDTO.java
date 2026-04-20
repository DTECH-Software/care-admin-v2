package com.dtech.admin.dto.response;

import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.Data;

import java.util.List;

@Data
public class EmployeeRejoinDetailsResponseDTO {
    private List<SimpleBaseDTO> previousCompanies;
    private List<String> previousEpfs;
}

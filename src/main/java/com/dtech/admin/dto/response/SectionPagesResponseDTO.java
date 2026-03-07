package com.dtech.admin.dto.response;

import lombok.Data;

import java.util.List;
@Data
public class SectionPagesResponseDTO {
    private String code;
    private String description;
    private List<WebPageResponseDTO> pages;
}
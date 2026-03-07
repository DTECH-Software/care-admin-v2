package com.dtech.admin.dto.api;

import lombok.Data;

@Data
public class ITextMessageRequestDTO {
    private String to;
    private String text;
}
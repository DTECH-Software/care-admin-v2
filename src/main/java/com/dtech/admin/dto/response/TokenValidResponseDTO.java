package com.dtech.admin.dto.response;

import lombok.Data;

@Data
public class TokenValidResponseDTO {
    private boolean valid;
    private String username;
}
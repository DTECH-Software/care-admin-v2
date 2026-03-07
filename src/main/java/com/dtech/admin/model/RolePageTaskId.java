package com.dtech.admin.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePageTaskId implements Serializable {
    private String roleCode;
    private String pageCode;
    private String taskCode;
}

package com.dtech.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependentBaseDTO extends SimpleBaseDTO{
    private String relationCategory;

    public DependentBaseDTO(String code, String description , String relationCategory) {
      super(code,description);
      this.relationCategory = relationCategory;

    }
}

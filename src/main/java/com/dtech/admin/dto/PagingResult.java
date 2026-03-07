package com.dtech.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagingResult<T> {
    private List<T> content;
    private long size;
    private long totalRecords;
}
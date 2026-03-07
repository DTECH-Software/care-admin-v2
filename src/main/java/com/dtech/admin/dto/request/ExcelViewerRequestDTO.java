package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExcelViewerRequestDTO extends ChannelRequestDTO{
    private Long id;
}

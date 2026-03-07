package com.dtech.admin.dto.response;

import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.Data;
import java.util.Date;

@Data
public class ExcelViewerResponseDTO {
    private Long id;
    private SimpleBaseDTO company;
    private String month;
    private String monthDescription;
    private SimpleBaseDTO year;
    private String status;
    private String statusDescription;
    private String rejectReason;
    private Date authDateTime;
    private String authUser;
}

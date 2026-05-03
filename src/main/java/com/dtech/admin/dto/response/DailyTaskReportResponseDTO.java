package com.dtech.admin.dto.response;

import lombok.Data;

@Data
public class DailyTaskReportResponseDTO {
    private String period;
    private DailyTaskMedicalRowDTO medical;
    private DailyTaskDdfRowDTO ddf;
}

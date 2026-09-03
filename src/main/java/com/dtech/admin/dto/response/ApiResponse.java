package com.dtech.admin.dto.response;

import com.dtech.admin.maintenance.MaintenanceResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.InputStreamResource;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private int errorCode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseTime;
    private boolean underMaintenance;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MaintenanceResponse maintenance;

    public ApiResponse(boolean success, String message, InputStreamResource data) {
        this.success = success;
        this.message = message;
        this.data = (T) data;
    }
}

package com.dtech.admin.util;

import com.dtech.admin.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ResponseStatus(HttpStatus.OK)
public class ResponseUtil {

    public <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setMessage(message);
        apiResponse.setData(data);
        apiResponse.setErrorCode(0);
        apiResponse.setErrors(null);
        apiResponse.setResponseTime(LocalDateTime.now());
        return apiResponse;
    }

    public  <T>ApiResponse<T> error(List<String> errors, int errorCode, String message) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(message);
        apiResponse.setData(null);
        apiResponse.setErrorCode(errorCode);
        apiResponse.setErrors(errors);
        apiResponse.setResponseTime(LocalDateTime.now());
        return apiResponse;
    }

}

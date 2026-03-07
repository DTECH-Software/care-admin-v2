/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 10:37 AM
 * <p>
 */

package com.dtech.admin.exception;


import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.util.ResponseUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.List;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler extends RuntimeException {

    @Autowired
    public ResponseUtil responseUtil;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentException(MethodArgumentNotValidException ex) {
        log.error("method argument exception {}",ex.getMessage());
        List<String> list = ex.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
        String message = String.join(", ", list);
        return ResponseEntity.internalServerError().body(responseUtil.error(list,1002,message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        log.error("exception {}",ex.getMessage());
        return ResponseEntity.internalServerError().body(responseUtil.error(Collections.singletonList(ex.getMessage()),1001,"Something went wrong. Please try again later or contact support if the issue persists"));
    }
}

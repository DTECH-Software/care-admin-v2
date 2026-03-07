package com.dtech.admin.service;

public interface AuditLogService {
    void log(String page,String task,String ipAddress,String taskDescription,String userAgent,String newValue,String oldValue,String username);
}

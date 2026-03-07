/**
 * User: Himal_J
 * Date: 4/23/2025
 * Time: 1:54 PM
 * <p>
 */

package com.dtech.admin.service.impl;

import com.dtech.admin.model.AuditLog;
import com.dtech.admin.model.WebPage;
import com.dtech.admin.model.WebTask;
import com.dtech.admin.repository.AuditLogRepository;
import com.dtech.admin.repository.WebPageRepository;
import com.dtech.admin.repository.WebTaskRepository;
import com.dtech.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private final WebTaskRepository webTaskRepository;

    @Autowired
    private final WebPageRepository webPageRepository;

    @Autowired
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = false)
    public void log(String page,String task,String taskDescription,String ipAddress,String userAgent,String newValue,String oldValue,String username) {
      try {
          log.info("Audit log");

          WebTask webTask = webTaskRepository.findByCode(task).orElse(null);
          WebPage webPage = webPageRepository.findByCode(page).orElse(null);

          log.info("Start audit log");
          AuditLog auditLog = new AuditLog();
          auditLog.setTask(webTask);
          auditLog.setTaskDescription(taskDescription);
          auditLog.setPage(webPage);
          auditLog.setIpAddress(ipAddress);
          auditLog.setUserAgent(userAgent);
          auditLog.setOldValue(oldValue);
          auditLog.setNewValue(newValue);
          auditLog.setCreatedBy(username != null ? username : "");
          auditLog.setLastModifiedBy(username != null ? username : "");
          log.info("End audit log page={} task={} user={}", page, task, username);
          auditLogRepository.saveAndFlush(auditLog);
          log.info("Audit log saved id={}", auditLog.getId());
      }catch (Exception e) {
          log.error(e);
          throw e;
      }
    }
}

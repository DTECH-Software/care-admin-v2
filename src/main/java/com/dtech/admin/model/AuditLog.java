/**
 * User: Himal_J
 * Date: 4/23/2025
 * Time: 2:04 PM
 * <p>
 */

package com.dtech.admin.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true, exclude = {"task", "page"})
@ToString(exclude = {"task", "page"})
@Entity
@Table(name = "audit_log")
@Data
public class AuditLog extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "source", nullable = false, updatable = false, length = 20)
    private String source = "WECARE_ADMIN";

    @Column(name = "module", updatable = false, length = 50)
    private String module;

    @Column(name = "action", updatable = false, length = 100)
    private String action;

    @Column(name = "result", updatable = false, length = 20)
    private String result;

    @Column(name = "response_status", updatable = false)
    private Integer responseStatus;

    @Column(name = "request_path", updatable = false, length = 255)
    private String requestPath;

    @Column(name = "http_method", updatable = false, length = 10)
    private String httpMethod;

    @Column(name = "duration_ms", updatable = false)
    private Long durationMs;

    @Column(name = "correlation_id", updatable = false, length = 64)
    private String correlationId;

    @Column(name = "client_app_version", updatable = false, length = 30)
    private String clientAppVersion;

    @Column(name = "client_platform", updatable = false, length = 20)
    private String clientPlatform;

    @Column(name = "app_update_status", updatable = false, length = 20)
    private String appUpdateStatus;

    @Column(columnDefinition = "json")
    private String oldValue;

    @Column(columnDefinition = "json")
    private String newValue;

    @Column(name = "ip_address",nullable = false,updatable = false)
    private String ipAddress;

    @Column(name = "user_agent",nullable = false,updatable = false)
    private String userAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task",nullable = false,updatable = false,referencedColumnName = "code")
    private WebTask task;

    @Column(name = "task_description",nullable = false,updatable = false)
    private String taskDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page",nullable = true,updatable = false,referencedColumnName = "code")
    private WebPage page;

}

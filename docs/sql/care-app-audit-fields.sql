-- Run after audit-log-all-activity.sql and before deploying Care-App auditing.

ALTER TABLE audit_log
    ADD COLUMN module VARCHAR(50) NULL AFTER source,
    ADD COLUMN action VARCHAR(100) NULL AFTER module,
    ADD COLUMN result VARCHAR(20) NULL AFTER action,
    ADD COLUMN response_status INT NULL AFTER result,
    ADD COLUMN request_path VARCHAR(255) NULL AFTER response_status,
    ADD COLUMN http_method VARCHAR(10) NULL AFTER request_path,
    ADD COLUMN duration_ms BIGINT NULL AFTER http_method,
    ADD COLUMN correlation_id VARCHAR(64) NULL AFTER duration_ms;

INSERT INTO web_task (code, description, status, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'API_REQUEST', 'Care-App API request', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_task WHERE code = 'API_REQUEST');

CREATE INDEX idx_audit_log_module_created_date ON audit_log (module, created_date);
CREATE INDEX idx_audit_log_result_created_date ON audit_log (result, created_date);
CREATE INDEX idx_audit_log_correlation_id ON audit_log (correlation_id);

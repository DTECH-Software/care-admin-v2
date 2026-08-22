-- Upgrade source: docs/sql/care-app-audit-fields.sql
-- Run after audit-log-all-activity.sql and before deploying Care-App auditing.

SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'module'), 'SELECT 1', 'ALTER TABLE audit_log ADD COLUMN module VARCHAR(50) NULL AFTER source');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'action'), 'SELECT 1', 'ALTER TABLE audit_log ADD COLUMN action VARCHAR(100) NULL AFTER module');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'result'), 'SELECT 1', 'ALTER TABLE audit_log ADD COLUMN result VARCHAR(20) NULL AFTER action');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'response_status'), 'SELECT 1', 'ALTER TABLE audit_log ADD COLUMN response_status INT NULL AFTER result');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'request_path'), 'SELECT 1', 'ALTER TABLE audit_log ADD COLUMN request_path VARCHAR(255) NULL AFTER response_status');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'http_method'), 'SELECT 1', 'ALTER TABLE audit_log ADD COLUMN http_method VARCHAR(10) NULL AFTER request_path');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'duration_ms'), 'SELECT 1', 'ALTER TABLE audit_log ADD COLUMN duration_ms BIGINT NULL AFTER http_method');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'correlation_id'), 'SELECT 1', 'ALTER TABLE audit_log ADD COLUMN correlation_id VARCHAR(64) NULL AFTER duration_ms');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO web_task (code, description, status, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'API_REQUEST', 'Care-App API request', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_task WHERE code = 'API_REQUEST');

SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_log_module_created_date'), 'SELECT 1', 'CREATE INDEX idx_audit_log_module_created_date ON audit_log (module, created_date)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_log_result_created_date'), 'SELECT 1', 'CREATE INDEX idx_audit_log_result_created_date ON audit_log (result, created_date)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_log_correlation_id'), 'SELECT 1', 'CREATE INDEX idx_audit_log_correlation_id ON audit_log (correlation_id)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

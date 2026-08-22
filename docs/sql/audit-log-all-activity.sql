-- All Activity audit page (ADIT_ALL) and common Admin/App source.
-- Run against the Care Admin schema before deploying this backend version.

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'source'),
    'SELECT 1',
    'ALTER TABLE audit_log ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT ''WECARE_ADMIN'' AFTER id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO web_section (code, description, status, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'ADIT', 'Audit Logs', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_section WHERE code = 'ADIT');

INSERT INTO web_page (code, url, description, status, section, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'ADIT_ALL', '/audit-logs/all-activity', 'All Activity', 'ACTIVE', 'ADIT',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_page WHERE code = 'ADIT_ALL');

INSERT INTO web_page_task (page_code, task_code)
SELECT 'ADIT_ALL', code FROM web_task WHERE code IN ('REF_DATA', 'SEARCH', 'VIEW')
AND NOT EXISTS (
    SELECT 1 FROM web_page_task wpt WHERE wpt.page_code = 'ADIT_ALL' AND wpt.task_code = web_task.code
);

-- Assign these read-only tasks to the required auditor/admin roles using the
-- existing role-page-task privilege screen. Do not assign ADD/UPDATE/DELETE.
-- Example (replace YOUR_AUDITOR_ROLE with the actual authorized role code):
-- INSERT INTO web_user_role_page_task (role_code, page_code, task_code)
-- SELECT 'YOUR_AUDITOR_ROLE', 'ADIT_ALL', task_code
-- FROM web_page_task
-- WHERE page_code = 'ADIT_ALL'
--   AND NOT EXISTS (
--       SELECT 1 FROM web_user_role_page_task assigned
--       WHERE assigned.role_code = 'YOUR_AUDITOR_ROLE'
--         AND assigned.page_code = 'ADIT_ALL'
--         AND assigned.task_code = web_page_task.task_code
--   );

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_log_source_created_date'),
    'SELECT 1',
    'CREATE INDEX idx_audit_log_source_created_date ON audit_log (source, created_date)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_log_created_user'),
    'SELECT 1',
    'CREATE INDEX idx_audit_log_created_user ON audit_log (created_user)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

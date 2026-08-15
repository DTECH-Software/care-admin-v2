-- Friendly source-specific audit pages.
-- Run after audit-log-all-activity.sql.

INSERT INTO web_section (code, description, status, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'ADIT', 'Audit Logs', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_section WHERE code = 'ADIT');

INSERT INTO web_page (code, url, description, status, section, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'ADIT_ADMIN', '/audit-logs/admin-activity', 'Admin Activity', 'ACTIVE', 'ADIT',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_page WHERE code = 'ADIT_ADMIN');

INSERT INTO web_page (code, url, description, status, section, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'ADIT_APP', '/audit-logs/care-app-activity', 'Care-App Activity', 'ACTIVE', 'ADIT',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_page WHERE code = 'ADIT_APP');

INSERT INTO web_page_task (page_code, task_code)
SELECT page_code, task_code
FROM (
    SELECT 'ADIT_ADMIN' AS page_code, 'REF_DATA' AS task_code
    UNION ALL SELECT 'ADIT_ADMIN', 'SEARCH'
    UNION ALL SELECT 'ADIT_ADMIN', 'VIEW'
    UNION ALL SELECT 'ADIT_APP', 'REF_DATA'
    UNION ALL SELECT 'ADIT_APP', 'SEARCH'
    UNION ALL SELECT 'ADIT_APP', 'VIEW'
) required_task
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task existing
    WHERE existing.page_code = required_task.page_code
      AND existing.task_code = required_task.task_code
);

-- Assign REF_DATA, SEARCH and VIEW only to approved auditor/admin roles through
-- User Role Privilege Management. Do not assign ADD, UPDATE or DELETE.

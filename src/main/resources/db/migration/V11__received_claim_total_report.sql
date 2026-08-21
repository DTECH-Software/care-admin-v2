-- Upgrade source: docs/sql/received-claim-total-report.sql
-- V1 installations do not consistently contain the Reports section.
INSERT INTO web_section (code, description, status, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'RPRT', 'Reports', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_section WHERE code = 'RPRT'
);

INSERT INTO web_page (code, url, description, status, section, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'RPRT_RCTR', '/reports/received-claim-total', 'Received Claim Total Report', 'ACTIVE', 'RPRT',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page WHERE code = 'RPRT_RCTR'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_RCTR', 'REF_DATA'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_RCTR' AND task_code = 'REF_DATA'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_RCTR', 'SEARCH'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_RCTR' AND task_code = 'SEARCH'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_RCTR', 'VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_RCTR' AND task_code = 'VIEW'
);

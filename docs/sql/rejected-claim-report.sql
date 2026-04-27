INSERT INTO web_page (code, url, description, status, section, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'RPRT_RCR', '/reports/rejected-claim', 'Rejected Claim Report', 'ACTIVE', 'RPRT',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page WHERE code = 'RPRT_RCR'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_RCR', 'REF_DATA'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_RCR' AND task_code = 'REF_DATA'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_RCR', 'SEARCH'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_RCR' AND task_code = 'SEARCH'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_RCR', 'VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_RCR' AND task_code = 'VIEW'
);

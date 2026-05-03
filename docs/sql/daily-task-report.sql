INSERT INTO web_page (code, url, description, status, section, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'RPRT_DTR', '/reports/daily-task', 'Daily Task Report', 'ACTIVE', 'RPRT',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page WHERE code = 'RPRT_DTR'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_DTR', 'REF_DATA'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_DTR' AND task_code = 'REF_DATA'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_DTR', 'SEARCH'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_DTR' AND task_code = 'SEARCH'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_DTR', 'VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_DTR' AND task_code = 'VIEW'
);

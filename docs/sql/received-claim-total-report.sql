INSERT INTO web_task (code, description, status, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'FILTER_LIST', 'Filter List', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_task WHERE code = 'FILTER_LIST'
);

INSERT INTO web_task (code, description, status, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'FILE_DOWNLOAD', 'File Download', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_task WHERE code = 'FILE_DOWNLOAD'
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
SELECT 'RPRT_RCTR', 'FILTER_LIST'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_RCTR' AND task_code = 'FILTER_LIST'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'RPRT_RCTR', 'FILE_DOWNLOAD'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'RPRT_RCTR' AND task_code = 'FILE_DOWNLOAD'
);

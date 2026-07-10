-- Add separate permissions for staff category update and transfer.
-- EMPM = Employee Details page, EPMP = Employee Management page.

INSERT INTO web_task (code, description, status, created_user, last_modified_user)
SELECT 'STAFF_CAT_UPDATE', 'Staff Category Update', 'ACTIVE', 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_task WHERE code = 'STAFF_CAT_UPDATE'
);

INSERT INTO web_task (code, description, status, created_user, last_modified_user)
SELECT 'STAFF_CAT_TRANSFER', 'Staff Category Transfer', 'ACTIVE', 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_task WHERE code = 'STAFF_CAT_TRANSFER'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT page_code, task_code
FROM (
    SELECT 'EMPM' AS page_code, 'STAFF_CAT_UPDATE' AS task_code
    UNION ALL SELECT 'EMPM', 'STAFF_CAT_TRANSFER'
    UNION ALL SELECT 'EPMP', 'STAFF_CAT_UPDATE'
    UNION ALL SELECT 'EPMP', 'STAFF_CAT_TRANSFER'
) tasks
WHERE NOT EXISTS (
    SELECT 1
    FROM web_page_task existing
    WHERE existing.page_code = tasks.page_code
      AND existing.task_code = tasks.task_code
);

-- Preserve existing behavior for roles that already had UPDATE permission on employee pages.
INSERT INTO web_user_role_page_task (role_code, page_code, task_code)
SELECT existing.role_code, existing.page_code, tasks.task_code
FROM web_user_role_page_task existing
JOIN (
    SELECT 'STAFF_CAT_UPDATE' AS task_code
    UNION ALL SELECT 'STAFF_CAT_TRANSFER'
) tasks
WHERE existing.page_code IN ('EMPM', 'EPMP')
  AND existing.task_code = 'UPDATE'
  AND NOT EXISTS (
      SELECT 1
      FROM web_user_role_page_task target
      WHERE target.role_code = existing.role_code
        AND target.page_code = existing.page_code
        AND target.task_code = tasks.task_code
  );

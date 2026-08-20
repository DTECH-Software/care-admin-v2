-- Upgrade source: docs/sql/employee-deactivate-permission.sql
-- Add DEACTIVATE as a separate selectable permission for Employee Details (EMPM)
-- and Employee Management (EPMP).

INSERT INTO web_task (code, description, status, created_user, last_modified_user)
SELECT 'DEACTIVATE', 'Deactivate', 'ACTIVE', 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_task WHERE code = 'DEACTIVATE'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'EMPM', 'DEACTIVATE'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'EMPM' AND task_code = 'DEACTIVATE'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'EPMP', 'DEACTIVATE'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'EPMP' AND task_code = 'DEACTIVATE'
);

-- Preserve existing behavior for roles that already had DELETE on these pages:
-- they will receive DEACTIVATE automatically.
INSERT INTO web_user_role_page_task (role_code, page_code, task_code)
SELECT existing.role_code, existing.page_code, 'DEACTIVATE'
FROM web_user_role_page_task existing
WHERE existing.page_code IN ('EMPM', 'EPMP')
  AND existing.task_code = 'DELETE'
  AND NOT EXISTS (
      SELECT 1
      FROM web_user_role_page_task target
      WHERE target.role_code = existing.role_code
        AND target.page_code = existing.page_code
        AND target.task_code = 'DEACTIVATE'
  );

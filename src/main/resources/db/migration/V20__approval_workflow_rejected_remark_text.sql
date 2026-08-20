-- Upgrade source: docs/sql/approval-workflow-rejected-remark-text.sql
ALTER TABLE approval_work_flow
    MODIFY COLUMN rejected_remak TEXT NULL;

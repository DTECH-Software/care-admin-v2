-- Read-only preflight for the complete V11-V44 Care Admin migration chain.
-- Run against the restored V1 database while Care Admin is stopped.

-- A genuine V1 production database has no Flyway history table yet. Report
-- that expected baseline state instead of aborting the remaining checks.
SET @preflight_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'flyway_schema_history'
    ),
    'SELECT ''FLYWAY_STATE'' AS check_group, installed_rank, version, description, success FROM flyway_schema_history WHERE CAST(SUBSTRING_INDEX(version, ''.'', 1) AS UNSIGNED) BETWEEN 9 AND 44 ORDER BY installed_rank',
    'SELECT ''FLYWAY_STATE'' AS check_group, ''NOT_PRESENT_BASELINE_EXPECTED'' AS state'
);
PREPARE preflight_stmt FROM @preflight_sql;
EXECUTE preflight_stmt;
DEALLOCATE PREPARE preflight_stmt;

WITH required_tables (table_name) AS (
    SELECT 'web_section' UNION ALL
    SELECT 'web_page' UNION ALL
    SELECT 'web_task' UNION ALL
    SELECT 'web_page_task' UNION ALL
    SELECT 'web_user_role_page_task' UNION ALL
    SELECT 'common_paramter' UNION ALL
    SELECT 'claims_request' UNION ALL
    SELECT 'user_company_details' UNION ALL
    SELECT 'document' UNION ALL
    SELECT 'approval_work_flow' UNION ALL
    SELECT 'notification_template' UNION ALL
    SELECT 'remark' UNION ALL
    SELECT 'audit_log' UNION ALL
    SELECT 'company_types' UNION ALL
    SELECT 'application_otp_sessions' UNION ALL
    SELECT 'application_user' UNION ALL
    SELECT 'insurance_staff_category_period' UNION ALL
    SELECT 'user_personal_details' UNION ALL
    SELECT 'death_claim_request' UNION ALL
    SELECT 'staff_category' UNION ALL
    SELECT 'insurance_policy'
)
SELECT 'REQUIRED_TABLE' AS check_group,
       required.table_name,
       IF(actual.table_name IS NULL, 'MISSING', 'OK') AS result
FROM required_tables required
LEFT JOIN information_schema.tables actual
       ON actual.table_schema = DATABASE()
      AND actual.table_name = required.table_name
ORDER BY required.table_name;

WITH required_columns (table_name, column_name) AS (
    SELECT 'web_section', 'code' UNION ALL
    SELECT 'web_page', 'code' UNION ALL
    SELECT 'web_page', 'section' UNION ALL
    SELECT 'web_task', 'code' UNION ALL
    SELECT 'web_page_task', 'page_code' UNION ALL
    SELECT 'web_page_task', 'task_code' UNION ALL
    SELECT 'web_user_role_page_task', 'role_code' UNION ALL
    SELECT 'web_user_role_page_task', 'page_code' UNION ALL
    SELECT 'web_user_role_page_task', 'task_code' UNION ALL
    SELECT 'common_paramter', 'code' UNION ALL
    SELECT 'common_paramter', 'value' UNION ALL
    SELECT 'claims_request', 'id' UNION ALL
    SELECT 'claims_request', 'employee' UNION ALL
    SELECT 'user_company_details', 'id' UNION ALL
    SELECT 'document', 'id' UNION ALL
    SELECT 'document', 'type' UNION ALL
    SELECT 'document', 'doc' UNION ALL
    SELECT 'document', 'file_type' UNION ALL
    SELECT 'approval_work_flow', 'id' UNION ALL
    SELECT 'approval_work_flow', 'approved_amount' UNION ALL
    SELECT 'approval_work_flow', 'rejected_remak' UNION ALL
    SELECT 'notification_template', 'type' UNION ALL
    SELECT 'notification_template', 'message_body' UNION ALL
    SELECT 'remark', 'id' UNION ALL
    SELECT 'remark', 'code' UNION ALL
    SELECT 'remark', 'description' UNION ALL
    SELECT 'remark', 'remark_category' UNION ALL
    SELECT 'audit_log', 'id' UNION ALL
    SELECT 'audit_log', 'created_date' UNION ALL
    SELECT 'audit_log', 'created_user' UNION ALL
    SELECT 'company_types', 'id' UNION ALL
    SELECT 'application_otp_sessions', 'id' UNION ALL
    SELECT 'application_otp_sessions', 'otp' UNION ALL
    SELECT 'application_otp_sessions', 'validated' UNION ALL
    SELECT 'application_otp_sessions', 'created_date' UNION ALL
    SELECT 'application_user', 'id' UNION ALL
    SELECT 'application_user', 'otp_session' UNION ALL
    SELECT 'application_user', 'primary_email' UNION ALL
    SELECT 'insurance_staff_category_period', 'id' UNION ALL
    SELECT 'user_personal_details', 'nic' UNION ALL
    SELECT 'user_personal_details', 'user_status' UNION ALL
    SELECT 'death_claim_request', 'id' UNION ALL
    SELECT 'staff_category', 'code' UNION ALL
    SELECT 'insurance_policy', 'id' UNION ALL
    SELECT 'insurance_policy', 'code'
)
SELECT 'REQUIRED_COLUMN' AS check_group,
       required.table_name,
       required.column_name,
       COALESCE(actual.column_type, 'MISSING') AS column_type,
       actual.character_set_name,
       actual.collation_name,
       actual.is_nullable
FROM required_columns required
LEFT JOIN information_schema.columns actual
       ON actual.table_schema = DATABASE()
      AND actual.table_name = required.table_name
      AND actual.column_name = required.column_name
ORDER BY required.table_name, required.column_name;

SELECT 'MENU_SECTION' AS check_group,
       code,
       description,
       status
FROM web_section
WHERE code = 'RPRT'
   OR LOWER(description) LIKE '%report%'
ORDER BY code;

SELECT 'MENU_TASK' AS check_group,
       required.code AS required_code,
       task.description,
       task.status,
       IF(task.code IS NULL, 'MISSING', 'OK') AS result
FROM (
    SELECT 'REF_DATA' AS code UNION ALL
    SELECT 'SEARCH' UNION ALL
    SELECT 'VIEW' UNION ALL
    SELECT 'ADD' UNION ALL
    SELECT 'UPDATE' UNION ALL
    SELECT 'DELETE' UNION ALL
    SELECT 'FILE_UPLOAD'
) required
LEFT JOIN web_task task ON task.code = required.code
ORDER BY required.code;

SELECT 'MENU_PAGE' AS check_group,
       required.code AS required_code,
       page.description,
       page.section,
       page.status,
       IF(page.code IS NULL, 'MISSING', 'OK') AS result
FROM (
    SELECT 'EMPM' AS code UNION ALL
    SELECT 'EPMP'
) required
LEFT JOIN web_page page ON page.code = required.code
ORDER BY required.code;

SELECT 'COMMON_PARAMETER' AS check_group,
       code,
       value
FROM common_paramter
WHERE code = 'EMPLOYEE_MAX_AGE_FOR_REQUEST_DDF';

SELECT 'NOTIFICATION_ENUM' AS check_group,
       column_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'notification_template'
  AND column_name = 'type';

SELECT 'NOTIFICATION_DATA' AS check_group,
       type,
       COUNT(*) AS row_count
FROM notification_template
GROUP BY type
ORDER BY type;

SELECT 'REFERENCE_COLUMN' AS check_group,
       table_name,
       column_name,
       column_type,
       character_set_name,
       collation_name,
       is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'document' AND column_name = 'id')
    OR (table_name = 'approval_work_flow' AND column_name = 'id')
    OR (table_name = 'company_types' AND column_name = 'id')
    OR (table_name = 'application_user' AND column_name = 'id')
    OR (table_name = 'insurance_staff_category_period' AND column_name = 'id')
    OR (table_name = 'claims_request' AND column_name = 'id')
    OR (table_name = 'death_claim_request' AND column_name = 'id')
    OR (table_name = 'staff_category' AND column_name = 'code')
    OR (table_name = 'insurance_policy' AND column_name = 'code'))
ORDER BY table_name, column_name;

SELECT 'TARGET_TABLE' AS check_group,
       target.table_name,
       IF(actual.table_name IS NULL, 'ABSENT', 'PRESENT') AS result,
       COALESCE(actual.table_rows, 0) AS estimated_rows
FROM (
    SELECT 'approval_workflow_reject_reason' AS table_name UNION ALL
    SELECT 'email_notification_event' UNION ALL
    SELECT 'email_notification_recipient_rule' UNION ALL
    SELECT 'support_ticket' UNION ALL
    SELECT 'support_ticket_message' UNION ALL
    SELECT 'support_ticket_attachment' UNION ALL
    SELECT 'support_ticket_status_history' UNION ALL
    SELECT 'application_user_biometrics' UNION ALL
    SELECT 'insurance_policy_staff_category_group' UNION ALL
    SELECT 'payment_attachment' UNION ALL
    SELECT 'payment_attachment_claim' UNION ALL
    SELECT 'payment_advice' UNION ALL
    SELECT 'payment_advice_attachment' UNION ALL
    SELECT 'payment_advice_death_claim' UNION ALL
    SELECT 'cheque_payment' UNION ALL
    SELECT 'cheque_payment_month' UNION ALL
    SELECT 'cheque_payment_document' UNION ALL
    SELECT 'cheque_payment_ddf' UNION ALL
    SELECT 'cheque_payment_ddf_month' UNION ALL
    SELECT 'cheque_payment_ddf_document'
) target
LEFT JOIN information_schema.tables actual
       ON actual.table_schema = DATABASE()
      AND actual.table_name = target.table_name
ORDER BY target.table_name;

SELECT 'TARGET_COLUMN' AS check_group,
       table_name,
       column_name,
       column_type,
       character_set_name,
       collation_name,
       is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'claims_request' AND column_name = 'assisted_mobile_no')
    OR (table_name = 'user_company_details' AND column_name IN
        ('transfer_date', 'transfer_doc', 'previous_staff_category', 'previous_insurance_policy'))
    OR (table_name = 'remark' AND column_name = 'include_in_rejected_claim_report')
    OR (table_name = 'audit_log' AND column_name IN
        ('source', 'module', 'action', 'result', 'response_status', 'request_path',
         'http_method', 'duration_ms', 'correlation_id'))
    OR (table_name = 'document' AND column_name IN
        ('storage_provider', 'bucket_name', 'object_key', 'object_size', 'checksum_sha256'))
    OR (table_name = 'application_otp_sessions' AND column_name IN
        ('purpose', 'application_user_id', 'context_key', 'consumed'))
    OR (table_name = 'approval_work_flow' AND column_name = 'policy_id')
    OR (table_name = 'user_personal_details' AND column_name = 'active_nic_key'))
ORDER BY table_name, ordinal_position;

SELECT 'ACTIVE_NIC_DUPLICATE_GROUPS' AS check_group,
       COUNT(*) AS issue_count
FROM (
    SELECT LOWER(TRIM(nic))
    FROM user_personal_details
    WHERE user_status = 'ACTIVE'
      AND nic IS NOT NULL
    GROUP BY LOWER(TRIM(nic))
    HAVING COUNT(*) > 1
) duplicate_nic;

SELECT 'INVALID_CLAIM_EMPLOYEES' AS check_group,
       COUNT(*) AS issue_count
FROM claims_request
WHERE employee IS NULL
   OR TRIM(CAST(employee AS CHAR)) = ''
   OR TRIM(CAST(employee AS CHAR)) NOT REGEXP '^[0-9]+$';

SELECT 'ORPHAN_CLAIM_EMPLOYEES' AS check_group,
       COUNT(*) AS issue_count
FROM claims_request claim
LEFT JOIN application_user app_user
       ON app_user.id = CAST(claim.employee AS UNSIGNED)
WHERE TRIM(CAST(claim.employee AS CHAR)) REGEXP '^[0-9]+$'
  AND app_user.id IS NULL;

SELECT 'APPLICATION_USER_PRIMARY_EMAIL_INDEX' AS check_group,
       index_name,
       non_unique,
       column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'application_user'
  AND column_name = 'primary_email'
ORDER BY index_name, seq_in_index;

SELECT 'LEGACY_APPLICATION_USER_OBJECT' AS check_group,
       'TRIGGER' AS object_type,
       trigger_name AS object_name
FROM information_schema.triggers
WHERE trigger_schema = DATABASE()
  AND trigger_name = 'trg_application_user_after_insert'
UNION ALL
SELECT 'LEGACY_APPLICATION_USER_OBJECT',
       routine_type,
       routine_name
FROM information_schema.routines
WHERE routine_schema = DATABASE()
  AND routine_name = 'sp_finalize_pending_claims_temp';

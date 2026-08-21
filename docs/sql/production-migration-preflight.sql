-- Read-only checks to run against the V1 database before enabling Flyway.

-- Must return zero rows before V39 can create the ACTIVE NIC unique key.
SELECT LOWER(TRIM(nic)) AS normalized_nic,
       COUNT(*) AS active_employee_count
FROM user_personal_details
WHERE user_status = 'ACTIVE'
  AND nic IS NOT NULL
GROUP BY LOWER(TRIM(nic))
HAVING COUNT(*) > 1;

-- All three claim checks must return zero before V40 converts employee to BIGINT
-- and creates the application_user foreign key.
SELECT id, request_id, employee
FROM claims_request
WHERE employee IS NULL
   OR TRIM(CAST(employee AS CHAR)) = ''
   OR TRIM(CAST(employee AS CHAR)) NOT REGEXP '^[0-9]+$';

SELECT claim.id, claim.request_id, claim.employee
FROM claims_request claim
LEFT JOIN application_user app_user
       ON app_user.id = CAST(claim.employee AS UNSIGNED)
WHERE TRIM(CAST(claim.employee AS CHAR)) REGEXP '^[0-9]+$'
  AND app_user.id IS NULL;

SELECT table_name,
       column_name,
       column_type,
       is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'claims_request' AND column_name = 'employee')
    OR (table_name = 'application_user' AND column_name = 'id'))
ORDER BY table_name, column_name;

-- Informational: legacy application-user primary-email indexes are removed by V40.
SELECT index_name,
       non_unique,
       column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'application_user'
  AND column_name = 'primary_email'
ORDER BY index_name, seq_in_index;

-- Confirm the migration-only routine/trigger state before V40 removes them.
SELECT trigger_name
FROM information_schema.triggers
WHERE trigger_schema = DATABASE()
  AND trigger_name = 'trg_application_user_after_insert';

SELECT routine_name,
       routine_type
FROM information_schema.routines
WHERE routine_schema = DATABASE()
  AND routine_name = 'sp_finalize_pending_claims_temp';

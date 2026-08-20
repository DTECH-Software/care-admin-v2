-- Read-only checks to run against the V1 database before enabling Flyway.

-- Must return zero rows before V39 can create the ACTIVE NIC unique key.
SELECT LOWER(TRIM(nic)) AS normalized_nic,
       COUNT(*) AS active_employee_count
FROM user_personal_details
WHERE user_status = 'ACTIVE'
GROUP BY LOWER(TRIM(nic))
HAVING COUNT(*) > 1;

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

-- The TEMP employee finalization routine and insert trigger were migration-only V1 objects.
DROP TRIGGER IF EXISTS trg_application_user_after_insert;
DROP PROCEDURE IF EXISTS sp_finalize_pending_claims_temp;

-- Some V1 databases created one or both primary-email indexes. Remove them only
-- when present so this migration also works on the currently deployed V1 schema.
SET @drop_primary_email_unique = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE application_user DROP INDEX `UKhglt2gle45gcv6dcqpi5nxovw`',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'application_user'
      AND index_name = 'UKhglt2gle45gcv6dcqpi5nxovw'
);
PREPARE application_user_index_stmt FROM @drop_primary_email_unique;
EXECUTE application_user_index_stmt;
DEALLOCATE PREPARE application_user_index_stmt;

-- Older V1 snapshots had the claims employee column without its foreign key.
-- Convert the migration-era character column to the final application_user key
-- type before adding the relationship. The production preflight must report no
-- TEMP/non-numeric/orphan employee values before this statement is run.
ALTER TABLE claims_request
    MODIFY COLUMN employee BIGINT NOT NULL;

SET @add_claim_employee_fk = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE claims_request ADD CONSTRAINT FK5n7ii3hrte1k4rggmg7eox0el FOREIGN KEY (employee) REFERENCES application_user(id)',
        'SELECT 1'
    )
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'claims_request'
      AND column_name = 'employee'
      AND referenced_table_name = 'application_user'
      AND referenced_column_name = 'id'
);
PREPARE application_user_index_stmt FROM @add_claim_employee_fk;
EXECUTE application_user_index_stmt;
DEALLOCATE PREPARE application_user_index_stmt;

SET @drop_primary_email_index = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE application_user DROP INDEX `idx_primary_email`',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'application_user'
      AND index_name = 'idx_primary_email'
);
PREPARE application_user_index_stmt FROM @drop_primary_email_index;
EXECUTE application_user_index_stmt;
DEALLOCATE PREPARE application_user_index_stmt;

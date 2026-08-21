-- Precondition: no two ACTIVE employees may have the same normalized NIC.
-- Run docs/sql/production-migration-preflight.sql before enabling Flyway.
SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_personal_details' AND column_name = 'active_nic_key'),
    'SELECT 1',
    'ALTER TABLE user_personal_details ADD COLUMN active_nic_key VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN user_status = ''ACTIVE'' THEN LOWER(TRIM(nic)) ELSE NULL END) STORED'
);
PREPARE v39_stmt FROM @ddl;
EXECUTE v39_stmt;
DEALLOCATE PREPARE v39_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user_personal_details' AND index_name = 'uk_user_personal_active_nic'),
    'SELECT 1',
    'CREATE UNIQUE INDEX uk_user_personal_active_nic ON user_personal_details (active_nic_key)'
);
PREPARE v39_stmt FROM @ddl;
EXECUTE v39_stmt;
DEALLOCATE PREPARE v39_stmt;

-- Mobile application version policy and Care-App audit metadata.
-- Version values use semantic version text (for example 1.2.3) and are compared by the application.

CREATE TABLE IF NOT EXISTS mobile_app_version_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    platform VARCHAR(20) NOT NULL,
    latest_version VARCHAR(30) NOT NULL,
    minimum_supported_version VARCHAR(30) NOT NULL,
    force_update TINYINT(1) NOT NULL DEFAULT 0,
    store_url VARCHAR(500) NULL,
    release_notes VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user VARCHAR(100) NULL,
    last_modified_user VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mobile_app_version_platform (platform)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'audit_log'
          AND column_name = 'client_app_version'
    ),
    'SELECT 1',
    'ALTER TABLE audit_log ADD COLUMN client_app_version VARCHAR(30) NULL AFTER user_agent'
);
PREPARE v45_stmt FROM @ddl;
EXECUTE v45_stmt;
DEALLOCATE PREPARE v45_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'audit_log'
          AND column_name = 'client_platform'
    ),
    'SELECT 1',
    'ALTER TABLE audit_log ADD COLUMN client_platform VARCHAR(20) NULL AFTER client_app_version'
);
PREPARE v45_stmt FROM @ddl;
EXECUTE v45_stmt;
DEALLOCATE PREPARE v45_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'audit_log'
          AND column_name = 'app_update_status'
    ),
    'SELECT 1',
    'ALTER TABLE audit_log ADD COLUMN app_update_status VARCHAR(20) NULL AFTER client_platform'
);
PREPARE v45_stmt FROM @ddl;
EXECUTE v45_stmt;
DEALLOCATE PREPARE v45_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'audit_log'
          AND index_name = 'idx_audit_log_app_version'
    ),
    'SELECT 1',
    'CREATE INDEX idx_audit_log_app_version ON audit_log (source, client_platform, client_app_version, created_date)'
);
PREPARE v45_stmt FROM @ddl;
EXECUTE v45_stmt;
DEALLOCATE PREPARE v45_stmt;

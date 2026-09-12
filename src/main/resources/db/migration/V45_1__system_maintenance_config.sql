-- Independent maintenance controls for Care App and Care Admin.
-- Version 45.1 intentionally leaves version 46 available for blocked mobile releases.

CREATE TABLE IF NOT EXISTS system_maintenance_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application VARCHAR(30) NOT NULL,
    maintenance_enabled TINYINT(1) NOT NULL DEFAULT 0,
    title VARCHAR(150) NULL,
    message VARCHAR(1000) NULL,
    start_at DATETIME NULL,
    end_at DATETIME NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user VARCHAR(100) NULL,
    last_modified_user VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_maintenance_application (application)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

INSERT IGNORE INTO system_maintenance_config
    (application, maintenance_enabled, title, message, created_user, last_modified_user)
VALUES
    ('CARE_APP', 0, 'System Maintenance',
     'WeCare App is temporarily unavailable due to scheduled maintenance.', 'SYSTEM', 'SYSTEM'),
    ('CARE_ADMIN', 0, 'System Maintenance',
     'WeCare Admin is temporarily unavailable due to scheduled maintenance.', 'SYSTEM', 'SYSTEM');

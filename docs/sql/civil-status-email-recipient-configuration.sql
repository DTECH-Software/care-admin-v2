-- Database-driven recipient routing for CIVIL_STATUS_MANAGEMENT emails.
-- CIVIL_STATUS_REJECTED remains inactive because the current system sends an SMS, not an email.

CREATE TABLE IF NOT EXISTS email_notification_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_user VARCHAR(255) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_notification_event_code (code),
    KEY idx_email_notification_event_category_status (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_notification_recipient_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    recipient_type VARCHAR(30) NOT NULL,
    recipient_code VARCHAR(100) NOT NULL,
    company_scope VARCHAR(30) NOT NULL DEFAULT 'ALL_COMPANIES',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_user VARCHAR(255) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_recipient_rule (event_id, recipient_type, recipient_code, company_scope),
    KEY idx_email_recipient_rule_event_status (event_id, status),
    CONSTRAINT fk_email_recipient_rule_event
        FOREIGN KEY (event_id) REFERENCES email_notification_event (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO email_notification_event
    (code, category, description, status, created_user, last_modified_user)
VALUES
    ('CIVIL_STATUS_SUBMITTED', 'CIVIL_STATUS_MANAGEMENT', 'Civil-status change submitted and pending HR approval', 'ACTIVE', 'system', 'system'),
    ('CIVIL_STATUS_APPROVED', 'CIVIL_STATUS_MANAGEMENT', 'Civil-status change approved by HR', 'ACTIVE', 'system', 'system'),
    ('CIVIL_STATUS_REJECTED', 'CIVIL_STATUS_MANAGEMENT', 'Civil-status change rejected by HR; current delivery channel is SMS', 'INACTIVE', 'system', 'system')
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    description = VALUES(description),
    last_modified_user = 'system';

INSERT IGNORE INTO email_notification_recipient_rule
    (event_id, recipient_type, recipient_code, company_scope, status, created_user, last_modified_user)
SELECT id, 'USER_ROLE', 'HRADMIN', 'SAME_COMPANY', 'ACTIVE', 'system', 'system'
FROM email_notification_event
WHERE code = 'CIVIL_STATUS_SUBMITTED';

INSERT IGNORE INTO email_notification_recipient_rule
    (event_id, recipient_type, recipient_code, company_scope, status, created_user, last_modified_user)
SELECT event.id,
       'USER_ROLE',
       role.code,
       'SAME_COMPANY_OR_UNASSIGNED',
       'ACTIVE',
       'system',
       'system'
FROM email_notification_event event
JOIN (
    SELECT 'SUPERADMIN1' AS code
    UNION ALL SELECT 'SUPERADMIN'
    UNION ALL SELECT 'ADMIN'
    UNION ALL SELECT 'APPROVER'
    UNION ALL SELECT 'DevTest'
    UNION ALL SELECT 'SubAdmin'
) role
WHERE event.code = 'CIVIL_STATUS_APPROVED';

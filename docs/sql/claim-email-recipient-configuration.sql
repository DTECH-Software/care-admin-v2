-- Database-driven recipient routing for CLAIM_WORKFLOW emails.
-- Existing behavior is preserved by seeding ALL_COMPANIES approval-level rules.
-- Change company_scope to SAME_COMPANY after confirming company assignments for all approvers.

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
    ('CLAIM_L1_APPROVED', 'CLAIM_WORKFLOW', 'Level 01 approved the claim', 'ACTIVE', 'system', 'system'),
    ('CLAIM_L1_REJECTED', 'CLAIM_WORKFLOW', 'Level 01 rejected the claim', 'ACTIVE', 'system', 'system'),
    ('CLAIM_L2_MATCHED_APPROVAL', 'CLAIM_WORKFLOW', 'Level 01 and Level 02 approval decisions matched', 'ACTIVE', 'system', 'system'),
    ('CLAIM_L2_DIFFERENT_DECISION', 'CLAIM_WORKFLOW', 'Level 01 and Level 02 decisions differ', 'ACTIVE', 'system', 'system'),
    ('CLAIM_L2_REJECTED_AFTER_L1_APPROVED', 'CLAIM_WORKFLOW', 'Level 02 rejected after Level 01 approval', 'ACTIVE', 'system', 'system'),
    ('CLAIM_L2_REJECTED_AFTER_L1_REJECTED', 'CLAIM_WORKFLOW', 'Level 02 rejected after Level 01 rejection', 'ACTIVE', 'system', 'system'),
    ('CLAIM_L3_FINAL_DECISION', 'CLAIM_WORKFLOW', 'Level 03 completed the final decision', 'ACTIVE', 'system', 'system')
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    description = VALUES(description),
    last_modified_user = 'system';

INSERT IGNORE INTO email_notification_recipient_rule
    (event_id, recipient_type, recipient_code, company_scope, status, created_user, last_modified_user)
SELECT id, 'APPROVAL_LEVEL', 'LEVEL02', 'ALL_COMPANIES', 'ACTIVE', 'system', 'system'
FROM email_notification_event
WHERE code IN ('CLAIM_L1_APPROVED', 'CLAIM_L1_REJECTED');

INSERT IGNORE INTO email_notification_recipient_rule
    (event_id, recipient_type, recipient_code, company_scope, status, created_user, last_modified_user)
SELECT id, 'APPROVAL_LEVEL', 'LEVEL03', 'ALL_COMPANIES', 'ACTIVE', 'system', 'system'
FROM email_notification_event
WHERE code IN ('CLAIM_L2_DIFFERENT_DECISION', 'CLAIM_L2_REJECTED_AFTER_L1_APPROVED');

INSERT IGNORE INTO email_notification_recipient_rule
    (event_id, recipient_type, recipient_code, company_scope, status, created_user, last_modified_user)
SELECT id, 'APPROVAL_LEVEL', 'LEVEL01', 'ALL_COMPANIES', 'ACTIVE', 'system', 'system'
FROM email_notification_event
WHERE code IN ('CLAIM_L2_MATCHED_APPROVAL', 'CLAIM_L2_REJECTED_AFTER_L1_REJECTED', 'CLAIM_L3_FINAL_DECISION');

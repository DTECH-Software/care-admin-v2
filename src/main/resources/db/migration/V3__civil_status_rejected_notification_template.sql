-- Upgrade source: docs/sql/civil-status-rejected-notification-template.sql
ALTER TABLE notification_template
MODIFY COLUMN type ENUM(
    'DEATH_CLAIM',
    'INSURANCE_CLAIM',
    'OTP',
    'USER_CREATION',
    'SENT_OTP_PASSWORD',
    'INSURANCE_APPROVAL',
    'INSURANCE_REJECTED',
    'DEATH_REJECTED',
    'DEATH_APPROVAL',
    'CIVIL_STATUS_REJECTED'
) NOT NULL;

INSERT INTO notification_template (
    type,
    title,
    message_body,
    email_body,
    created_date,
    last_modified_date,
    created_user,
    last_modified_user
) SELECT
    'CIVIL_STATUS_REJECTED',
    'Civil Status Rejected',
    'Automated HR notification: Your marriage certificate was rejected. Please review via the WeCare app: https://wecare.dsi.lk/care-app/auth/login or contact your HR/supervisor. Thank you.',
    NULL,
    NOW(),
    NOW(),
    'system',
    'system'
WHERE NOT EXISTS (
    SELECT 1
    FROM notification_template
    WHERE type = 'CIVIL_STATUS_REJECTED'
);

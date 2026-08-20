-- Upgrade source: docs/sql/civil-status-rejected-notification-template.sql
INSERT INTO notification_template (
    type,
    title,
    message_body,
    email_body,
    created_date,
    last_modified_date,
    created_user,
    last_modified_user
) VALUES (
    'CIVIL_STATUS_REJECTED',
    'Civil Status Rejected',
    'Automated HR notification: Your marriage certificate was rejected. Please review via the WeCare app: https://wecare.dsi.lk/care-app/auth/login or contact your HR/supervisor. Thank you.',
    NULL,
    NOW(),
    NOW(),
    'system',
    'system'
);

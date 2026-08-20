-- Upgrade source: docs/sql/insurance-partial-approval-notification-template.sql
ALTER TABLE notification_template
MODIFY COLUMN type ENUM(
    'DEATH_CLAIM',
    'INSURANCE_CLAIM',
    'OTP',
    'USER_CREATION',
    'SENT_OTP_PASSWORD',
    'INSURANCE_APPROVAL',
    'INSURANCE_PARTIAL_APPROVAL',
    'INSURANCE_REJECTED',
    'DEATH_REJECTED',
    'DEATH_APPROVAL',
    'CIVIL_STATUS_REJECTED',
    'DEPENDENT_REJECTED',
    'CIVIL_STATUS_APPROVED',
    'DEPENDENT_APPROVED'
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
)
SELECT
    'INSURANCE_PARTIAL_APPROVAL',
    'Insurance Claim Partially Approved',
    'Your claim has now been partially approved under Request ID: {0} for an amount of {1}.\nThank you!',
    NULL,
    NOW(),
    NOW(),
    'system',
    'system'
WHERE NOT EXISTS (
    SELECT 1
    FROM notification_template
    WHERE type = 'INSURANCE_PARTIAL_APPROVAL'
);

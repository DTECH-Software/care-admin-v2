UPDATE notification_template
SET message_body = 'Your One-Time Password (OTP) for verification is {0}. For security reasons, do not share your OTP with anyone. Thank you!',
    last_modified_date = NOW(),
    last_modified_user = 'system'
WHERE type = 'USER_CREATION';

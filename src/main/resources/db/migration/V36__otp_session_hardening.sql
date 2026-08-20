-- Care App OTP session hardening. Existing rows remain readable.
ALTER TABLE application_otp_sessions
    ADD COLUMN purpose VARCHAR(50) NULL AFTER otp,
    ADD COLUMN application_user_id BIGINT NULL AFTER purpose,
    ADD COLUMN context_key VARCHAR(255) NULL AFTER application_user_id,
    ADD COLUMN consumed BIT(1) NOT NULL DEFAULT b'0' AFTER validated;

CREATE INDEX idx_otp_session_user_purpose_created
    ON application_otp_sessions (application_user_id, purpose, created_date);

CREATE INDEX idx_otp_session_context_purpose_created
    ON application_otp_sessions (context_key, purpose, created_date);

CREATE INDEX idx_otp_session_code_purpose
    ON application_otp_sessions (otp, purpose, validated, consumed);

-- Preserve the user association for currently linked legacy OTP sessions.
UPDATE application_otp_sessions otp_session
JOIN application_user app_user ON app_user.otp_session = otp_session.id
SET otp_session.application_user_id = app_user.id,
    otp_session.purpose = COALESCE(otp_session.purpose, 'LEGACY')
WHERE otp_session.application_user_id IS NULL;

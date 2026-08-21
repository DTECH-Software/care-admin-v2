-- Care App OTP session hardening. Existing rows remain readable.
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'application_otp_sessions' AND column_name = 'purpose'), 'SELECT 1', 'ALTER TABLE application_otp_sessions ADD COLUMN purpose VARCHAR(50) NULL AFTER otp');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'application_otp_sessions' AND column_name = 'application_user_id'), 'SELECT 1', 'ALTER TABLE application_otp_sessions ADD COLUMN application_user_id BIGINT NULL AFTER purpose');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'application_otp_sessions' AND column_name = 'context_key'), 'SELECT 1', 'ALTER TABLE application_otp_sessions ADD COLUMN context_key VARCHAR(255) NULL AFTER application_user_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'application_otp_sessions' AND column_name = 'consumed'), 'SELECT 1', 'ALTER TABLE application_otp_sessions ADD COLUMN consumed BIT(1) NOT NULL DEFAULT b''0'' AFTER validated');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'application_otp_sessions' AND index_name = 'idx_otp_session_user_purpose_created'), 'SELECT 1', 'CREATE INDEX idx_otp_session_user_purpose_created ON application_otp_sessions (application_user_id, purpose, created_date)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'application_otp_sessions' AND index_name = 'idx_otp_session_context_purpose_created'), 'SELECT 1', 'CREATE INDEX idx_otp_session_context_purpose_created ON application_otp_sessions (context_key, purpose, created_date)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'application_otp_sessions' AND index_name = 'idx_otp_session_code_purpose'), 'SELECT 1', 'CREATE INDEX idx_otp_session_code_purpose ON application_otp_sessions (otp, purpose, validated, consumed)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Preserve the user association for currently linked legacy OTP sessions.
UPDATE application_otp_sessions otp_session
JOIN application_user app_user ON app_user.otp_session = otp_session.id
SET otp_session.application_user_id = app_user.id,
    otp_session.purpose = COALESCE(otp_session.purpose, 'LEGACY')
WHERE otp_session.application_user_id IS NULL;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'claims_request' AND column_name = 'assisted_mobile_no'),
    'SELECT 1',
    'ALTER TABLE claims_request ADD COLUMN assisted_mobile_no VARCHAR(20) NULL'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

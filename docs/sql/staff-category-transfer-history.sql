SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_company_details' AND column_name = 'previous_staff_category'),
    'SELECT 1',
    'ALTER TABLE user_company_details ADD COLUMN previous_staff_category VARCHAR(255) NULL'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_company_details' AND column_name = 'previous_insurance_policy'),
    'SELECT 1',
    'ALTER TABLE user_company_details ADD COLUMN previous_insurance_policy VARCHAR(255) NULL'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.key_column_usage WHERE table_schema = DATABASE() AND table_name = 'user_company_details' AND column_name = 'previous_staff_category' AND referenced_table_name = 'staff_category' AND referenced_column_name = 'code'),
    'SELECT 1',
    'ALTER TABLE user_company_details ADD CONSTRAINT fk_user_company_details_previous_staff_category FOREIGN KEY (previous_staff_category) REFERENCES staff_category (code)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.key_column_usage WHERE table_schema = DATABASE() AND table_name = 'user_company_details' AND column_name = 'previous_insurance_policy' AND referenced_table_name = 'insurance_policy' AND referenced_column_name = 'code'),
    'SELECT 1',
    'ALTER TABLE user_company_details ADD CONSTRAINT fk_user_company_details_previous_insurance_policy FOREIGN KEY (previous_insurance_policy) REFERENCES insurance_policy (code)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

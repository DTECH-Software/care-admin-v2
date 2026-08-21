SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_company_details' AND column_name = 'transfer_date'),
    'SELECT 1',
    'ALTER TABLE user_company_details ADD COLUMN transfer_date DATE NULL'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_company_details' AND column_name = 'transfer_doc'),
    'SELECT 1',
    'ALTER TABLE user_company_details ADD COLUMN transfer_doc BIGINT NULL'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = 'user_company_details'
          AND column_name = 'transfer_doc'
          AND referenced_table_name = 'document'
          AND referenced_column_name = 'id'
    ),
    'SELECT 1',
    'ALTER TABLE user_company_details ADD CONSTRAINT fk_user_company_details_transfer_doc FOREIGN KEY (transfer_doc) REFERENCES document (id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

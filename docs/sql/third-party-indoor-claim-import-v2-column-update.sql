-- V8 creates the latest table shape for new installations. These guarded
-- changes also support databases that already have the earlier table shape.

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'third_party_indoor_claim_batch_row' AND column_name = 'policy_no'),
    'SELECT 1',
    'ALTER TABLE third_party_indoor_claim_batch_row ADD COLUMN policy_no VARCHAR(100) NULL AFTER policy_year'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'third_party_indoor_claim_batch_row' AND column_name = 'intimated_date'),
    'SELECT 1',
    'ALTER TABLE third_party_indoor_claim_batch_row ADD COLUMN intimated_date DATE NULL AFTER to_date'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'third_party_indoor_claim_batch_row' AND column_name = 'paid_date'),
    'SELECT 1',
    'ALTER TABLE third_party_indoor_claim_batch_row ADD COLUMN paid_date DATE NULL AFTER intimated_date'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'third_party_indoor_claim_batch_row' AND column_name = 'non_payable_amount'),
    'SELECT 1',
    'ALTER TABLE third_party_indoor_claim_batch_row ADD COLUMN non_payable_amount DECIMAL(18,2) NULL AFTER paid_date'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'third_party_indoor_claim_batch_row' AND column_name = 'non_payable_item'),
    'SELECT 1',
    'ALTER TABLE third_party_indoor_claim_batch_row ADD COLUMN non_payable_item VARCHAR(1000) NULL AFTER non_payable_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'third_party_indoor_claim_batch_row' AND column_name = 'claim_amount'),
    'SELECT 1',
    'ALTER TABLE third_party_indoor_claim_batch_row ADD COLUMN claim_amount DECIMAL(18,2) NULL AFTER non_payable_item'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE third_party_indoor_claim_batch_row
SET policy_no = COALESCE(policy_no, 'MIGRATED'),
    intimated_date = COALESCE(intimated_date, from_date),
    paid_date = COALESCE(paid_date, to_date),
    non_payable_amount = COALESCE(non_payable_amount, 0.00),
    claim_amount = COALESCE(claim_amount, approved_amount),
    approved_amount = COALESCE(approved_amount, claim_amount);

ALTER TABLE third_party_indoor_claim_batch_row
    MODIFY COLUMN policy_no VARCHAR(100) NOT NULL,
    MODIFY COLUMN intimated_date DATE NOT NULL,
    MODIFY COLUMN paid_date DATE NOT NULL,
    MODIFY COLUMN non_payable_amount DECIMAL(18,2) NOT NULL,
    MODIFY COLUMN claim_amount DECIMAL(18,2) NOT NULL;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'third_party_indoor_claim_batch_row' AND column_name = 'hospital'),
    'ALTER TABLE third_party_indoor_claim_batch_row DROP COLUMN hospital',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'third_party_indoor_claim_batch_row' AND column_name = 'disease'),
    'ALTER TABLE third_party_indoor_claim_batch_row DROP COLUMN disease',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

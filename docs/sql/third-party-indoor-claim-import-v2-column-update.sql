ALTER TABLE third_party_indoor_claim_batch_row
    ADD COLUMN policy_no VARCHAR(100) NULL AFTER policy_year,
    ADD COLUMN intimated_date DATE NULL AFTER to_date,
    ADD COLUMN paid_date DATE NULL AFTER intimated_date,
    ADD COLUMN non_payable_amount DECIMAL(18,2) NULL AFTER paid_date,
    ADD COLUMN non_payable_item VARCHAR(1000) NULL AFTER non_payable_amount,
    ADD COLUMN claim_amount DECIMAL(18,2) NULL AFTER non_payable_item;

UPDATE third_party_indoor_claim_batch_row
SET policy_no = COALESCE(policy_no, 'MIGRATED'),
    intimated_date = COALESCE(intimated_date, from_date),
    paid_date = COALESCE(paid_date, to_date),
    non_payable_amount = COALESCE(non_payable_amount, 0.00),
    claim_amount = COALESCE(claim_amount, request_amount);

ALTER TABLE third_party_indoor_claim_batch_row
    MODIFY COLUMN policy_no VARCHAR(100) NOT NULL,
    MODIFY COLUMN intimated_date DATE NOT NULL,
    MODIFY COLUMN paid_date DATE NOT NULL,
    MODIFY COLUMN non_payable_amount DECIMAL(18,2) NOT NULL,
    MODIFY COLUMN claim_amount DECIMAL(18,2) NOT NULL;

ALTER TABLE third_party_indoor_claim_batch_row
    DROP COLUMN hospital,
    DROP COLUMN disease,
    DROP COLUMN request_amount;

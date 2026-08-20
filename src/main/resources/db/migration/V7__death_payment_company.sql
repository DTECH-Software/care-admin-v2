-- Upgrade source: docs/sql/death-payment-company-migration.sql
ALTER TABLE user_company_details
    ADD COLUMN death_payment_company VARCHAR(50) NULL AFTER payment_company;

ALTER TABLE user_company_details
    ADD CONSTRAINT fk_user_company_details_death_payment_company
        FOREIGN KEY (death_payment_company) REFERENCES company_types(code);

UPDATE user_company_details
SET death_payment_company = payment_company
WHERE death_payment_company IS NULL
  AND payment_company IS NOT NULL;

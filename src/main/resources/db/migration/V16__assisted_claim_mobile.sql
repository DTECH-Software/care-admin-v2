-- Upgrade source: docs/sql/assisted-claim-mobile.sql
ALTER TABLE claims_request
    ADD COLUMN assisted_mobile_no VARCHAR(20) NULL;

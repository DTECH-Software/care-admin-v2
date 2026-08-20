-- Upgrade source: docs/sql/staff-category-transfer-history.sql
ALTER TABLE user_company_details
    ADD COLUMN previous_staff_category VARCHAR(255) NULL,
    ADD COLUMN previous_insurance_policy VARCHAR(255) NULL;

ALTER TABLE user_company_details
    ADD CONSTRAINT fk_user_company_details_previous_staff_category
        FOREIGN KEY (previous_staff_category) REFERENCES staff_category (code);

ALTER TABLE user_company_details
    ADD CONSTRAINT fk_user_company_details_previous_insurance_policy
        FOREIGN KEY (previous_insurance_policy) REFERENCES insurance_policy (code);

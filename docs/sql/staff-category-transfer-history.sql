ALTER TABLE user_company_details
    ADD COLUMN previous_staff_category VARCHAR(255) NULL;

ALTER TABLE user_company_details
    ADD CONSTRAINT fk_user_company_details_previous_staff_category
        FOREIGN KEY (previous_staff_category) REFERENCES staff_category (code);

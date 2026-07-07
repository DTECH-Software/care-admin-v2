ALTER TABLE user_company_details
    ADD COLUMN transfer_date DATE NULL,
    ADD COLUMN transfer_doc BIGINT NULL;

ALTER TABLE user_company_details
    ADD CONSTRAINT fk_user_company_details_transfer_doc
        FOREIGN KEY (transfer_doc) REFERENCES document (id);

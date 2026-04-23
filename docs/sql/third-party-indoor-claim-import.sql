CREATE TABLE IF NOT EXISTS third_party_indoor_claim_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_no VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(255) NULL,
    total_rows INT NOT NULL DEFAULT 0,
    valid_rows INT NOT NULL DEFAULT 0,
    invalid_rows INT NOT NULL DEFAULT 0,
    duplicate_rows INT NOT NULL DEFAULT 0,
    imported_rows INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_user VARCHAR(255) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_third_party_indoor_claim_batch_no (batch_no)
);

CREATE TABLE IF NOT EXISTS third_party_indoor_claim_batch_row (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    row_no INT NOT NULL,
    external_reference_no VARCHAR(255) NOT NULL,
    company_code VARCHAR(50) NOT NULL,
    epf_no VARCHAR(100) NOT NULL,
    employee_name VARCHAR(255) NULL,
    policy_year INT NOT NULL,
    policy_no VARCHAR(100) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    intimated_date DATE NOT NULL,
    paid_date DATE NOT NULL,
    non_payable_amount DECIMAL(18,2) NOT NULL,
    non_payable_item VARCHAR(1000) NULL,
    claim_amount DECIMAL(18,2) NOT NULL,
    approved_amount DECIMAL(18,2) NOT NULL,
    remark VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(2000) NULL,
    insurance_claim_id BIGINT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_user VARCHAR(255) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    KEY idx_third_party_indoor_claim_batch_row_batch_id (batch_id),
    KEY idx_third_party_indoor_claim_batch_row_claim_id (insurance_claim_id),
    KEY idx_third_party_indoor_claim_batch_row_external_reference_no (external_reference_no),
    CONSTRAINT fk_third_party_indoor_claim_batch_row_batch
        FOREIGN KEY (batch_id) REFERENCES third_party_indoor_claim_batch (id),
    CONSTRAINT fk_third_party_indoor_claim_batch_row_claim
        FOREIGN KEY (insurance_claim_id) REFERENCES claims_request (id)
);

INSERT INTO web_section (code, description, status, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'TPCM', 'Third Party Claims', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_section WHERE code = 'TPCM'
);

INSERT INTO web_page (code, url, description, status, section, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'TPIC', '/third-party-indoor-claims', 'Third Party Indoor Claim Import', 'ACTIVE', 'TPCM',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page WHERE code = 'TPIC'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'TPIC', 'REF_DATA'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'TPIC' AND task_code = 'REF_DATA'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'TPIC', 'SEARCH'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'TPIC' AND task_code = 'SEARCH'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'TPIC', 'VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'TPIC' AND task_code = 'VIEW'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'TPIC', 'ADD'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'TPIC' AND task_code = 'ADD'
);

INSERT INTO web_page_task (page_code, task_code)
SELECT 'TPIC', 'FILE_UPLOAD'
WHERE NOT EXISTS (
    SELECT 1 FROM web_page_task WHERE page_code = 'TPIC' AND task_code = 'FILE_UPLOAD'
);

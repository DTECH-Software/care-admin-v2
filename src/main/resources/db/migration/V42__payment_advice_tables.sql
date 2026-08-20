CREATE TABLE IF NOT EXISTS payment_advice (
    id BIGINT NOT NULL AUTO_INCREMENT,
    advice_no VARCHAR(255) NOT NULL,
    advice_year_start INT NOT NULL,
    advice_year_end INT NOT NULL,
    advice_sequence INT NOT NULL,
    voucher_no VARCHAR(255) NOT NULL,
    voucher_sequence INT NOT NULL,
    advice_type VARCHAR(32) NULL,
    company_code VARCHAR(255) NOT NULL,
    staff_category_code VARCHAR(255) NOT NULL,
    department VARCHAR(255) NULL,
    insurance VARCHAR(255) NULL,
    total_requested_amount DECIMAL(16,2) NULL,
    total_approved_amount DECIMAL(16,2) NULL,
    status VARCHAR(32) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_advice_no (advice_no),
    UNIQUE KEY uk_payment_advice_voucher_no (voucher_no),
    KEY idx_payment_advice_status (status),
    KEY idx_payment_advice_company_staff (company_code, staff_category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS payment_advice_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_advice_id BIGINT NOT NULL,
    payment_attachment_id BIGINT NOT NULL,
    attachment_no VARCHAR(255) NOT NULL,
    request_amount DECIMAL(16,2) NULL,
    approved_amount DECIMAL(16,2) NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_advice_attachment_source (payment_attachment_id),
    KEY idx_payment_advice_attachment_advice (payment_advice_id),
    CONSTRAINT fk_payment_advice_attachment_advice
        FOREIGN KEY (payment_advice_id) REFERENCES payment_advice (id),
    CONSTRAINT fk_payment_advice_attachment_source
        FOREIGN KEY (payment_attachment_id) REFERENCES payment_attachment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS payment_advice_death_claim (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_advice_id BIGINT NOT NULL,
    death_claim_id BIGINT NOT NULL,
    request_id VARCHAR(255) NOT NULL,
    approved_amount DECIMAL(16,2) NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_advice_death_claim_source (death_claim_id),
    KEY idx_payment_advice_death_claim_advice (payment_advice_id),
    CONSTRAINT fk_payment_advice_death_claim_advice
        FOREIGN KEY (payment_advice_id) REFERENCES payment_advice (id),
    CONSTRAINT fk_payment_advice_death_claim_source
        FOREIGN KEY (death_claim_id) REFERENCES death_claim_request (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

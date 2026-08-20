CREATE TABLE IF NOT EXISTS cheque_payment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_code VARCHAR(255) NOT NULL,
    staff_category_code VARCHAR(255) NOT NULL,
    cheque_year VARCHAR(255) NOT NULL,
    cheque_no VARCHAR(255) NOT NULL,
    cheque_bank VARCHAR(255) NULL,
    cheque_branch VARCHAR(255) NULL,
    cheque_date DATE NULL,
    amount DECIMAL(16,2) NULL,
    received_date DATE NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_cheque_payment_company_staff (company_code, staff_category_code),
    KEY idx_cheque_payment_year (cheque_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS cheque_payment_month (
    cheque_payment_id BIGINT NOT NULL,
    month VARCHAR(255) NOT NULL,
    KEY idx_cheque_payment_month_payment (cheque_payment_id),
    CONSTRAINT fk_cheque_payment_month_payment
        FOREIGN KEY (cheque_payment_id) REFERENCES cheque_payment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS cheque_payment_document (
    cheque_payment_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    KEY idx_cheque_payment_document_payment (cheque_payment_id),
    KEY idx_cheque_payment_document_document (document_id),
    CONSTRAINT fk_cheque_payment_document_payment
        FOREIGN KEY (cheque_payment_id) REFERENCES cheque_payment (id),
    CONSTRAINT fk_cheque_payment_document_document
        FOREIGN KEY (document_id) REFERENCES document (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS cheque_payment_ddf (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_code VARCHAR(255) NOT NULL,
    staff_category_code VARCHAR(255) NOT NULL,
    cheque_year VARCHAR(255) NOT NULL,
    cheque_no VARCHAR(255) NOT NULL,
    cheque_bank VARCHAR(255) NULL,
    cheque_branch VARCHAR(255) NULL,
    cheque_date DATE NULL,
    amount DECIMAL(16,2) NULL,
    received_date DATE NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_cheque_payment_ddf_company_staff (company_code, staff_category_code),
    KEY idx_cheque_payment_ddf_year (cheque_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS cheque_payment_ddf_month (
    cheque_payment_id BIGINT NOT NULL,
    month VARCHAR(255) NOT NULL,
    KEY idx_cheque_payment_ddf_month_payment (cheque_payment_id),
    CONSTRAINT fk_cheque_payment_ddf_month_payment
        FOREIGN KEY (cheque_payment_id) REFERENCES cheque_payment_ddf (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS cheque_payment_ddf_document (
    cheque_payment_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    KEY idx_cheque_payment_ddf_document_payment (cheque_payment_id),
    KEY idx_cheque_payment_ddf_document_document (document_id),
    CONSTRAINT fk_cheque_payment_ddf_document_payment
        FOREIGN KEY (cheque_payment_id) REFERENCES cheque_payment_ddf (id),
    CONSTRAINT fk_cheque_payment_ddf_document_document
        FOREIGN KEY (document_id) REFERENCES document (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

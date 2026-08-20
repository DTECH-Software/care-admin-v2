CREATE TABLE IF NOT EXISTS application_user_biometrics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    unique_code VARCHAR(2048) NULL,
    app_id VARCHAR(255) NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    last_used_date DATETIME NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_application_user_biometrics_user_id (user_id),
    CONSTRAINT fk_application_user_biometrics_user
        FOREIGN KEY (user_id) REFERENCES application_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

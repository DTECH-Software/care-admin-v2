CREATE TABLE IF NOT EXISTS insurance_policy_staff_category_group (
    id BIGINT NOT NULL AUTO_INCREMENT,
    insurance_policy BIGINT NOT NULL,
    staff_category VARCHAR(255) NOT NULL,
    main_category_code VARCHAR(100) NOT NULL,
    main_category_description VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_user VARCHAR(255) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_insurance_policy_staff_category_group_policy_staff (insurance_policy, staff_category),
    KEY idx_insurance_policy_staff_category_group_main_code (main_category_code),
    CONSTRAINT fk_ins_policy_staff_group_policy
        FOREIGN KEY (insurance_policy) REFERENCES insurance_policy (id),
    CONSTRAINT fk_ins_policy_staff_group_staff
        FOREIGN KEY (staff_category) REFERENCES staff_category (code)
);

-- Example inserts after identifying the correct policy IDs:
-- INSERT INTO insurance_policy_staff_category_group
--     (insurance_policy, staff_category, main_category_code, main_category_description, status, created_user, last_modified_user)
-- VALUES
--     (42, 'EX-OP1', 'EXOP', 'Executive Staff - Options 01 & 02', 'ACTIVE', 'admin', 'admin'),
--     (42, 'EX-OP2', 'EXOP', 'Executive Staff - Options 01 & 02', 'ACTIVE', 'admin', 'admin');

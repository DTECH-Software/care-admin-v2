-- Precondition: no two ACTIVE employees may have the same normalized NIC.
-- Run docs/sql/production-migration-preflight.sql before enabling Flyway.
ALTER TABLE user_personal_details
    ADD COLUMN active_nic_key VARCHAR(255)
        GENERATED ALWAYS AS (
            CASE
                WHEN user_status = 'ACTIVE' THEN LOWER(TRIM(nic))
                ELSE NULL
            END
        ) STORED,
    ADD CONSTRAINT uk_user_personal_active_nic UNIQUE (active_nic_key);

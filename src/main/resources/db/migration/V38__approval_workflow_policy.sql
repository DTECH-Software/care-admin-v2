ALTER TABLE approval_work_flow
    ADD COLUMN policy_id BIGINT NULL AFTER approved_amount,
    ADD INDEX fk_approval_work_flow_policy (policy_id),
    ADD CONSTRAINT fk_approval_work_flow_policy
        FOREIGN KEY (policy_id) REFERENCES insurance_staff_category_period (id);

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'approval_work_flow' AND column_name = 'policy_id'),
    'SELECT 1',
    'ALTER TABLE approval_work_flow ADD COLUMN policy_id BIGINT NULL AFTER approved_amount'
);
PREPARE v38_stmt FROM @ddl;
EXECUTE v38_stmt;
DEALLOCATE PREPARE v38_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'approval_work_flow' AND index_name = 'idx_approval_work_flow_policy'),
    'SELECT 1',
    'CREATE INDEX idx_approval_work_flow_policy ON approval_work_flow (policy_id)'
);
PREPARE v38_stmt FROM @ddl;
EXECUTE v38_stmt;
DEALLOCATE PREPARE v38_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = 'approval_work_flow'
          AND column_name = 'policy_id'
          AND referenced_table_name = 'insurance_staff_category_period'
          AND referenced_column_name = 'id'
    ),
    'SELECT 1',
    'ALTER TABLE approval_work_flow ADD CONSTRAINT fk_approval_work_flow_policy FOREIGN KEY (policy_id) REFERENCES insurance_staff_category_period (id)'
);
PREPARE v38_stmt FROM @ddl;
EXECUTE v38_stmt;
DEALLOCATE PREPARE v38_stmt;

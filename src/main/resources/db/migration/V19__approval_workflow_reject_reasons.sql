-- Upgrade source: docs/sql/approval-workflow-reject-reasons.sql
CREATE TABLE IF NOT EXISTS approval_workflow_reject_reason (
    id BIGINT NOT NULL AUTO_INCREMENT,
    approval_workflow_id BIGINT NOT NULL,
    reason_code VARCHAR(100) NOT NULL,
    reason_description VARCHAR(255) NOT NULL,
    reason_category VARCHAR(100) NULL,
    amount DECIMAL(16,2) NOT NULL,
    remark VARCHAR(500) NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_awrr_workflow (approval_workflow_id),
    CONSTRAINT fk_awrr_workflow
        FOREIGN KEY (approval_workflow_id) REFERENCES approval_work_flow(id)
);

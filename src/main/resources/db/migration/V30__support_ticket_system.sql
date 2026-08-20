-- Upgrade source: docs/sql/support-ticket-system.sql
-- Support ticket system for Care Admin users and the central support team.
-- This script is idempotent and does not assign permissions to any role.

CREATE TABLE IF NOT EXISTS support_ticket (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_no VARCHAR(40) NOT NULL,
    system_type VARCHAR(30) NOT NULL,
    company_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    resolution TEXT NULL,
    resolved_date DATETIME NULL,
    closed_date DATETIME NULL,
    record_version BIGINT NOT NULL DEFAULT 0,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_ticket_no (ticket_no),
    KEY idx_support_ticket_system_status (system_type, status),
    KEY idx_support_ticket_company (company_id),
    KEY idx_support_ticket_created_date (created_date),
    CONSTRAINT fk_support_ticket_company FOREIGN KEY (company_id) REFERENCES company_types (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_ticket_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    author_username VARCHAR(100) NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_support_ticket_message_ticket (ticket_id, created_date),
    CONSTRAINT fk_support_ticket_message_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_ticket_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    message_id BIGINT NULL,
    document_id BIGINT NOT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_support_ticket_attachment_ticket (ticket_id),
    KEY idx_support_ticket_attachment_message (message_id),
    KEY idx_support_ticket_attachment_document (document_id),
    CONSTRAINT fk_support_ticket_attachment_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id),
    CONSTRAINT fk_support_ticket_attachment_message FOREIGN KEY (message_id) REFERENCES support_ticket_message (id),
    CONSTRAINT fk_support_ticket_attachment_document FOREIGN KEY (document_id) REFERENCES document (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_ticket_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    remark VARCHAR(1000) NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(255) NOT NULL,
    last_modified_user VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_support_ticket_history_ticket (ticket_id, created_date),
    CONSTRAINT fk_support_ticket_history_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO web_section (code, description, status, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'SUPT', 'Support Tickets', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_section WHERE code = 'SUPT');

INSERT INTO web_page (code, url, description, status, section, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'SUP_ADMIN', '/support-tickets/wecare-admin', 'WeCare Admin', 'ACTIVE', 'SUPT',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_page WHERE code = 'SUP_ADMIN');

INSERT INTO web_page (code, url, description, status, section, created_date, last_modified_date, created_user, last_modified_user)
SELECT 'SUP_APP', '/support-tickets/wecare-app', 'WeCare App', 'ACTIVE', 'SUPT',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM web_page WHERE code = 'SUP_APP');

INSERT INTO web_page_task (page_code, task_code)
SELECT required_task.page_code, required_task.task_code
FROM (
    SELECT 'SUP_ADMIN' AS page_code, 'REF_DATA' AS task_code
    UNION ALL SELECT 'SUP_ADMIN', 'SEARCH'
    UNION ALL SELECT 'SUP_ADMIN', 'VIEW'
    UNION ALL SELECT 'SUP_ADMIN', 'ADD'
    UNION ALL SELECT 'SUP_ADMIN', 'UPDATE'
    UNION ALL SELECT 'SUP_ADMIN', 'FILE_UPLOAD'
    UNION ALL SELECT 'SUP_APP', 'REF_DATA'
    UNION ALL SELECT 'SUP_APP', 'SEARCH'
    UNION ALL SELECT 'SUP_APP', 'VIEW'
    UNION ALL SELECT 'SUP_APP', 'ADD'
    UNION ALL SELECT 'SUP_APP', 'UPDATE'
    UNION ALL SELECT 'SUP_APP', 'FILE_UPLOAD'
) required_task
WHERE EXISTS (SELECT 1 FROM web_task task WHERE task.code = required_task.task_code)
  AND NOT EXISTS (
      SELECT 1 FROM web_page_task existing
      WHERE existing.page_code = required_task.page_code
        AND existing.task_code = required_task.task_code
  );

-- Recommended role setup through User Role Privilege Management:
-- Care Admin: REF_DATA, SEARCH, VIEW, ADD and FILE_UPLOAD.
-- Central Support: REF_DATA, SEARCH, VIEW, ADD, UPDATE and FILE_UPLOAD.
-- UPDATE controls status changes. ADD controls both ticket creation and replies.

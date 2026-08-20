-- Upgrade source: docs/sql/system-user-company-migration.sql
CREATE TABLE IF NOT EXISTS web_user_company (
    web_user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    PRIMARY KEY (web_user_id, company_id),
    CONSTRAINT fk_web_user_company_user
        FOREIGN KEY (web_user_id) REFERENCES web_user(id),
    CONSTRAINT fk_web_user_company_company
        FOREIGN KEY (company_id) REFERENCES company_types(id)
);

INSERT INTO web_user_company (web_user_id, company_id)
SELECT wu.id, ct.id
FROM web_user wu
CROSS JOIN company_types ct
LEFT JOIN web_user_company wuc
    ON wuc.web_user_id = wu.id
   AND wuc.company_id = ct.id
WHERE wu.status IN ('ACTIVE', 'INACTIVE')
  AND ct.status = 'ACTIVE'
  AND wuc.web_user_id IS NULL;

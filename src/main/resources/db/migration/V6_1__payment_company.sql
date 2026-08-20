-- Some V1 installations do not yet have the medical-claim payment company.
SET @add_payment_company_column = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE user_company_details ADD COLUMN payment_company VARCHAR(255) NULL AFTER promo_doc',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_company_details'
      AND column_name = 'payment_company'
);
PREPARE payment_company_stmt FROM @add_payment_company_column;
EXECUTE payment_company_stmt;
DEALLOCATE PREPARE payment_company_stmt;

SET @add_payment_company_fk = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE user_company_details ADD CONSTRAINT fk_user_company_details_payment_company FOREIGN KEY (payment_company) REFERENCES company_types(code)',
        'SELECT 1'
    )
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'user_company_details'
      AND constraint_name = 'fk_user_company_details_payment_company'
);
PREPARE payment_company_stmt FROM @add_payment_company_fk;
EXECUTE payment_company_stmt;
DEALLOCATE PREPARE payment_company_stmt;

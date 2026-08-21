SET @add_reject_report_column = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE remark ADD COLUMN include_in_rejected_claim_report BOOLEAN NOT NULL DEFAULT TRUE',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'remark'
      AND column_name = 'include_in_rejected_claim_report'
);
PREPARE reject_report_column_stmt FROM @add_reject_report_column;
EXECUTE reject_report_column_stmt;
DEALLOCATE PREPARE reject_report_column_stmt;

-- Set FALSE for reasons that must not appear or be counted in the
-- Reject Reason Report breakdown. Rejected claim totals remain unchanged.
-- Example:
-- UPDATE remark
-- SET include_in_rejected_claim_report = FALSE
-- WHERE remark_category = 'INSURANCE' AND code IN ('REASON_CODE');

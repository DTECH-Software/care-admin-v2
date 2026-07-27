ALTER TABLE remark
    ADD COLUMN include_in_rejected_claim_report BOOLEAN NOT NULL DEFAULT TRUE;

-- Set FALSE for reasons that must not appear or be counted in the
-- Reject Reason Report breakdown. Rejected claim totals remain unchanged.
-- Example:
-- UPDATE remark
-- SET include_in_rejected_claim_report = FALSE
-- WHERE remark_category = 'INSURANCE' AND code IN ('REASON_CODE');

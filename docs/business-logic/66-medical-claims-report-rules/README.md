# 66. Medical Claims Report Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the business logic used by the Medical Claims Report.

## Business Summary
Medical Claims Report is a finalized medical payment/reporting view. It is not the same as Claim Status Report because it is based on final decision and finalized payment attachment logic.

## Main Business Rules
- The report includes medical claims with final status APPROVED or REJECTED.
- The report excludes UNDER_REVIEW claims.
- Date range is based on final decision date, not claim created date.
- Approved claims use the L2 or L3 final approval date.
- Rejected claims use the L2 or L3 rejection date.
- Claims must have finalized payment attachment to appear in the report.
- Final remark should show L2 or L3 final remark, not L1-only remarks.
- Advice number and payment advice generated/not generated status are shown where available.
- Excel wording uses Voucher No where the business output requires it.
- Treatment category is available as a filter and an Excel output column.

## BA Review Points
- Confirm the report should remain final-decision based, not created-date based.
- Confirm finalized payment attachment requirement.
- Confirm final remark source for approved and rejected claims.
- Confirm Voucher No wording.

## QA Checkpoints
- Compare with Claim Status Report and verify expected differences by date basis.
- Test approved, rejected, partial approved, and under-review claims.
- Test treatment category filter in filter-list and export.
- Verify export uses the same filters as filter-list.

## Client View
- This report is for finalized medical claim outcomes, not all created claims.

## Related Topics
- 45. Insurance Claim Approval Workflow
- 63. Payment Advice Generation
- 83. Claim Status Report Rules

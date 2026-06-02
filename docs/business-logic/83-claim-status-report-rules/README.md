# 83. Claim Status Report Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how Claim Status Report rows and filters work.

## Business Summary
Claim Status Report is a claim activity report. It is based on claim created date and can show claims across statuses, including under-review claims.

## Main Business Rules
- Date range is based on claim created date.
- The report can show UNDER_REVIEW, APPROVED, and REJECTED claims unless filtered by status.
- The report does not require payment attachment or payment advice to exist.
- Report columns include Date, Company, Staff Category, EPF Number, Employee Name, Dependent Name, Dependent Category, Treatment Type, Treatment Category, Request Amount, Approved Amount, Claim Status, and Final Remark.
- Treatment category is available in filter-list and export.
- Export must use the same filter payload and filtering behavior as filter-list.
- Final Remark should use final L2 or L3 remark where available.

## BA Review Points
- Confirm that this report should remain created-date based.
- Confirm which statuses should be shown by default.
- Confirm whether payment advice status should remain outside this report.

## QA Checkpoints
- Filter by created date, treatment category, company, staff category, and status.
- Verify export does not ignore filters.
- Compare with Medical Claims Report and confirm differences are expected.

## Client View
- This report shows claim status by claim activity date, not payment or final decision date.

## Related Topics
- 66. Medical Claims Report Rules
- 45. Insurance Claim Approval Workflow

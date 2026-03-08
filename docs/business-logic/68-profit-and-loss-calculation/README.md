# 68. Profit and Loss Calculation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of profit and loss calculation so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how profit and loss calculation rule affects payment and reporting outputs.

## Main Business Rules
- Only intended claim statuses should be included in payment and report outputs.
- Totals and detail rows should come from the same approved data.
- Identifiers, remarks, and company context should stay readable.
- Outputs should remain aligned with approval and payment statuses.

## BA Review Points
- Confirm which statuses are eligible for payment or reporting.
- Confirm output layout and exported values.
- Confirm which fields are mandatory in client-facing outputs.

## QA Checkpoints
- Test on-screen and exported outputs.
- Verify totals and row-level values match the claim data.
- Verify repeated generation stays correct.

## Client View
- Payment and report outputs should be easy for business users to read.
- Client-facing outputs should match approved claim records.

## Related Topics
- 67. DDF Report Rules
- 69. Notification Template Usage

# 88. Payment Company and Original Company Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain when the system uses original company and when it uses payment company.

## Business Summary
Original company identifies where the employee or claim belongs. Payment company identifies the company used for payment processing. Some reports and documents must show both.

## Main Business Rules
- Original company comes from the employee company at claim context unless a historical claim snapshot overrides it.
- Payment company comes from medical payment company or death payment company configuration.
- Medical payment attachment and payment advice can show both original company and payment company.
- DDF payment advice default company and description should use original company context.
- DDF payment company should show the configured death payment company where available.
- PNL grouping uses payment advice company code, which can represent payment company rather than original company.
- Reports comparing claim totals to payment totals can differ when original company and payment company are not the same.
- Long company names should wrap in PDF/Excel outputs instead of being truncated.

## BA Review Points
- Confirm which reports should group by original company and which should group by payment company.
- Confirm display labels: Original Company, Payment Company, Default Company.
- Confirm behavior for missing payment company.

## QA Checkpoints
- Test employee with original company different from payment company.
- Verify payment attachment, payment advice, DDF advice, and PNL outputs.
- Verify long company names in generated documents.

## Client View
- Users should be able to distinguish where the claim came from and which company pays it.

## Related Topics
- 61. Payment Attachment Creation
- 63. Payment Advice Generation
- 64. Payment Advice Death Generation
- 68. Profit and Loss Calculation

# 64. Payment Advice Death Generation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how DDF payment advice is generated and displayed.

## Business Summary
DDF payment advice is generated for finalized death donation claim payment processing and uses DDF-specific company and description rules.

## Main Business Rules
- DDF payment advice uses death claim payment data, not medical claim treatment data.
- Default company in the DDF advice output should use original company context.
- Payment company should show the configured death payment company where available.
- Description should use original company context so the business reason is clear.
- Long details and descriptions must wrap to additional rows rather than being truncated.
- Rejected DDF claims are tracked separately in daily task payment-completed counts where required.

## BA Review Points
- Confirm exact wording for Default Company, Payment Company, and Details/Description.
- Confirm rejected DDF claim handling in payment-related reporting.
- Confirm whether original company or payment company drives finance grouping.

## QA Checkpoints
- Generate DDF advice with different original and payment companies.
- Verify long description wraps and remains readable.
- Verify DDF advice totals are not mixed with medical advice totals unless report type includes both.

## Client View
- DDF payment advice should clearly show the original company and payment company without cutting off text.

## Related Topics
- 65. DDF Payment Advice Rules
- 85. Daily Task Report Rules
- 88. Payment Company and Original Company Rules

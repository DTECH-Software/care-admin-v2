# 61. Payment Attachment Creation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how medical payment attachments are created and which claim/company fields they use.

## Business Summary
Payment attachment groups approved or rejected medical claims for payment processing. Draft attachments do not complete the process; finalized attachments are used by reports and downstream payment advice.

## Main Business Rules
- Payment attachments are created from eligible medical claims after final claim decision.
- Approved and rejected claims can appear in payment attachment flows where business reporting requires both settled outcomes.
- Draft payment attachments do not reduce pending payment work in daily task reporting.
- Only finalized payment attachments are treated as completed for medical claim report and daily task report payment-preparation logic.
- Payment attachment output can show both original company and payment company.
- In the payment attachment table, company display should distinguish original company and payment company where both are required.
- Long company names and descriptions must wrap instead of being cut off in generated outputs.

## BA Review Points
- Confirm which rejected claim statuses should be eligible for payment attachment reporting.
- Confirm where original company vs payment company should appear.
- Confirm whether draft attachments should be visible but not counted as completed.

## QA Checkpoints
- Create draft payment attachment and verify daily task report still shows pending payment work.
- Finalize payment attachment and verify pending payment work reduces.
- Verify original company and payment company fields for EX-OP grouped categories.
- Verify long text wraps in generated attachment output.

## Client View
- Payment attachment should be readable and should not mark work as completed until finalized.

## Related Topics
- 62. Payment Attachment Status Changes
- 63. Payment Advice Generation
- 88. Payment Company and Original Company Rules

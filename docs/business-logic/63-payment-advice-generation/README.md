# 63. Payment Advice Generation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how medical payment advice is generated from finalized payment attachment data.

## Business Summary
Payment advice is the payment-facing document generated from finalized medical payment attachment data. It carries company, staff category, amount, and claim identifiers used by finance users.

## Main Business Rules
- Medical payment advice is generated from finalized payment attachment data.
- Payment advice status must be finalized before PNL treats it as paid.
- Payment advice totals use total approved amount; if that value is null, fallback logic may use total requested amount.
- A zero approved amount is a valid value and is not treated as null.
- Voucher number wording is used in Excel where cheque number was previously shown for medical claim report output.
- Original company and payment company can be different and must be displayed where required.
- Payment advice generated status is shown in medical claim report outputs.

## BA Review Points
- Confirm whether PNL should use payment advice created date or finalized date.
- Confirm whether total approved amount should always be recalculated from attached claim rows before finalization.
- Confirm display wording for voucher number and payment company.

## QA Checkpoints
- Generate advice from finalized attachment and verify totals.
- Verify advice with approved amount 0.00 remains 0.00 and does not fallback to requested amount.
- Verify PNL includes only finalized advice.
- Verify medical claim report shows generated/not generated status.

## Client View
- Finance users should see final payment advice values that match finalized claim payment decisions.

## Related Topics
- 61. Payment Attachment Creation
- 68. Profit and Loss Calculation
- 88. Payment Company and Original Company Rules

# 68. Profit and Loss Calculation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how the Profit and Loss report calculates total received, total paid, difference, and result.

## Business Summary
PNL compares money received through cheque payments against money paid through finalized payment advice. It does not calculate directly from claim created date or claim final decision date.

## Main Business Rules
- Total Paid is calculated from finalized payment advice records.
- Total Paid uses payment advice created date for year/month filtering.
- Total Paid uses total approved amount when present.
- If total approved amount is null, total requested amount is used as fallback.
- A total approved amount of 0.00 is treated as a real value and is not replaced by requested amount.
- Medical PNL includes MEDICAL payment advice records and legacy records where type is null where the schema supports type.
- Older schemas without payment advice type can mix DDF and medical unless report logic separates them by advice rules.
- DDF PNL uses DEATH payment advice records where type is available.
- Total Received is calculated from cheque payment records and allocated across selected months.
- Difference is Total Received minus Total Paid.
- Positive difference is Profit, negative difference is Loss, zero is Breakeven.

## BA Review Points
- Confirm whether month filtering should use advice created date or finalized date.
- Confirm whether paid amount should always be recalculated from attached claim rows.
- Confirm medical vs DDF separation for older schemas without advice type.
- Confirm whether company grouping should use payment company or original company.

## QA Checkpoints
- Compare PNL Total Paid against finalized payment advice totals for the selected month.
- Verify DDF advice is not included in medical-only totals.
- Verify 0.00 approved advice remains 0.00.
- Verify cheque payment month allocation for Total Received.

## Client View
- PNL shows finance movement by payment advice and cheque receipt timing, not claim decision timing.

## Related Topics
- 63. Payment Advice Generation
- 64. Payment Advice Death Generation
- 88. Payment Company and Original Company Rules

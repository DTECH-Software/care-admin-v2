# 36. Staff Category Change Carry Forward Logic

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of staff category change carry forward logic so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how staff category change carry forward logic rule is applied during claim request, balance, and claim processing.

## Main Business Rules
- When staff category changes, the new category does not inherit all historical usage.
- Only the old staff-category period active on the change date should carry forward into the new category balance.
- Older expired periods should not keep reducing the new category balance.
- If staff category does not change, normal same-period logic applies.

## BA Review Points
- Confirm the exact decision rule and its input data.
- Confirm the business message when the rule fails.
- Confirm which screens and APIs must follow the same rule.

## QA Checkpoints
- Test normal, boundary, and invalid-input scenarios.
- Verify the same result appears across related flows.
- Verify returned messages match the business outcome.

## Client View
- Claim results should be explainable from policy and employee context.
- Users should understand why a claim is allowed, limited, or blocked.

## Related Topics
- 35. Available Balance Calculation
- 37. Claim Request Date Validation

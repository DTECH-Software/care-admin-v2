# 35. Available Balance Calculation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of available balance calculation so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how available balance calculation rule is applied during claim request, balance, and claim processing.

## Main Business Rules
- Available balance is fund limit minus approved usage in the relevant bucket.
- The bucket may be a single category, a shared group, or a parent-child bucket.
- Rejected claims should not reduce the available balance.
- Available balance must never be negative.

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
- 34. Nested Bucket Logic
- 36. Staff Category Change Carry Forward Logic

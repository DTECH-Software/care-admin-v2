# 33. Shared Bucket Logic

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of shared bucket logic so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how shared bucket logic rule is applied during claim request, balance, and claim processing.

## Main Business Rules
- Categories with the same fund limit under one treatment share one usage bucket.
- Approved usage in one shared category reduces the other shared categories.
- All categories in the same shared bucket should show the same remaining amount.
- Rejected claims should not reduce the shared bucket.

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
- 32. Treatment Category Bucket Structure
- 34. Nested Bucket Logic

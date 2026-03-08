# 34. Nested Bucket Logic

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of nested bucket logic so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how nested bucket logic rule is applied during claim request, balance, and claim processing.

## Main Business Rules
- A smaller category limit behaves as a child bucket inside a larger parent bucket.
- Child usage reduces both child and parent.
- Parent usage reduces parent and can cap the child remaining amount.
- Child available balance must never exceed parent remaining balance.

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
- 33. Shared Bucket Logic
- 35. Available Balance Calculation

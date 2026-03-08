# 32. Treatment Category Bucket Structure

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of treatment category bucket structure so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how treatment category bucket structure rule is applied during claim request, balance, and claim processing.

## Main Business Rules
- The rule should be applied before a claim is accepted or finalized.
- Configured policy, period, treatment, and category data should drive the result.
- The same business meaning should appear across request, summary, approval, and history where relevant.
- When the rule blocks a claim, the message should be clear to business users.

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
- 31. Global Limit vs Quarter Limit Rule
- 33. Shared Bucket Logic

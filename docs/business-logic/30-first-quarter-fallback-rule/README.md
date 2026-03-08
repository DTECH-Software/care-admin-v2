# 30. First Quarter Fallback Rule

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of first quarter fallback rule so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how first quarter fallback rule rule is applied during claim request, balance, and claim processing.

## Main Business Rules
- If permanent date is before the first configured quarter, the first quarter is used.
- The system should not jump directly to global limit when quarter setup exists and this fallback applies.
- The fallback should be consistent across screens using quarter logic.
- This rule matters only when quarter-based control is enabled.

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
- 29. Permanent Date to Quarter Selection
- 31. Global Limit vs Quarter Limit Rule

# 31. Global Limit vs Quarter Limit Rule

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of global limit vs quarter limit rule so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how global limit vs quarter limit rule rule is applied during claim request, balance, and claim processing.

## Main Business Rules
- If quarter control is disabled, global limit is used.
- If quarter control is enabled, the selected quarter and category amount must be used.
- Global limit should not override quarter logic for quarter-based treatments.
- The same rule should appear in request, summary, dashboard, and approval behavior.

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
- 30. First Quarter Fallback Rule
- 32. Treatment Category Bucket Structure

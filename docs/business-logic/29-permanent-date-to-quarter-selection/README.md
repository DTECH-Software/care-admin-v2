# 29. Permanent Date to Quarter Selection

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the current business understanding of permanent date to quarter selection so BA, QA, and client-side reviewers can validate the rule in the same language.

## Business Summary
Explains how permanent date to quarter selection rule is applied during claim request, balance, and claim processing.

## Main Business Rules
- Quarter selection uses the employee permanent date when permanent-date-based logic is required.
- If the permanent date falls inside a quarter range, that quarter is selected.
- The same quarter result should be used consistently in request, summary, and approval views.
- Quarter selection should be explainable from employee data and configured quarter rows.

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
- 28. Policy Period Selection
- 30. First Quarter Fallback Rule
